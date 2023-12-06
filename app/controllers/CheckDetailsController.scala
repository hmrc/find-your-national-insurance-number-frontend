/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.FrontendAppConfig
import connectors.IndividualDetailsConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.IndividualDetailsResponseEnvelope.IndividualDetailsResponseEnvelope
import models.errors.IndividualDetailsError
import models.individualdetails.AccountStatusType._
import models.individualdetails.AddressStatus._
import models.individualdetails.CrnIndicator._
import models.individualdetails.AddressType._
import models.individualdetails.{Address, AddressList, IndividualDetails, ResolveMerge}
import models.pdv.{PDVRequest, PDVResponseData}
import models.{CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope, Mode}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditService, PersonalDetailsValidationService}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.AuditUtils
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckDetailsController @Inject()(
      override val messagesApi: MessagesApi,
      identify: IdentifierAction,
      getData: DataRetrievalAction,
      requireData: DataRequiredAction,
      personalDetailsValidationService: PersonalDetailsValidationService,
      auditService: AuditService,
      individualDetailsConnector: IndividualDetailsConnector,
      val controllerComponents: MessagesControllerComponents,
      val authConnector: AuthConnector
    )(implicit ec: ExecutionContext, appConfig: FrontendAppConfig)
  extends FrontendBaseController with AuthorisedFunctions with I18nSupport with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) async {
    implicit request => {

      lazy val toAuthCredentialId: Option[Credentials] => Future[Option[String]] =
        (credentials: Option[Credentials]) => Future.successful(credentials.map(_.providerId))

      for {
        credentialId <- authorised().retrieve(credentials)(toAuthCredentialId).recover { case _ => None }
        pdvRequest = PDVRequest(credentialId.getOrElse(""), request.session.data.getOrElse("sessionId", ""))
        pdvData <- getPDVData(pdvRequest)
        idData <- getIdData(pdvData)
      } yield (pdvData, idData) match {

        case (
          pdvData @ PDVResponseData(id, validationStatus, personalDetails, _, _, _, _),
          Right(idData @ IndividualDetails(_, _, accountStatusType, _, _, _, _, _, _, crnIndicator, _, addressList))
          ) =>
            auditService.audit(AuditUtils.buildAuditEvent(personalDetails, "StartFindYourNino",
              validationStatus, crnIndicator.asString, id, None, None, None, None))

            val orConditions =
              validationStatus.equals("failure") ||
                !accountStatusType.exists(_.equals(FullLive)) ||
                  crnIndicator.equals(True) ||
                    !getAddressTypeResidential(addressList).addressStatus.exists(_.equals(NotDlo))

            if(orConditions)
              Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
            else {
              val idPostCode = getNPSPostCode(idData)
              if (pdvData.getPostCode.nonEmpty){
                if (idPostCode.equals(pdvData.getPostCode))
                  Redirect(routes.ValidDataNINOHelpController.onPageLoad(mode = mode))
                else
                  Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
              } else {
                //personalDetailsValidationService.updatePDVDataRowWithNPSPostCode(pdvData.getNino, idPostCode)
                Redirect(routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(mode = mode))
              }
            }


//        case (pdvData: PDVResponseData, Right(idData)) => {
//          idData match {
//            case individualDetailsData => {
//              auditService.audit(AuditUtils.buildAuditEvent(pdvData.personalDetails, "StartFindYourNino",
//                pdvData.validationStatus, individualDetailsData.crnIndicator.asString, pdvData.id, None, None, None, None))
//              if (pdvData.getPostCode.nonEmpty) {
//                checkConditions(individualDetailsData, pdvData.getPostCode) match {
//                  case (true, reason) => {
//                    personalDetailsValidationService.updatePDVDataRowWithValidationStatus(pdvData.id, true, reason)
//                    Redirect(routes.ValidDataNINOHelpController.onPageLoad(mode = mode))
//                  }
//                  case (false, reason) => {
//                    personalDetailsValidationService.updatePDVDataRowWithValidationStatus(pdvData.id, false, reason)
//                    Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
//                  }
//                }
//              } else {
//                Redirect(routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(mode = mode))
//              }
//            }
//            case _ => Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
//          }
//        }
      }
    }
  }

  def getIdData(pdvData: PDVResponseData)(implicit hc: HeaderCarrier): Future[Either[IndividualDetailsError, IndividualDetails]] = {
    getIndividualDetails(IndividualDetailsNino(pdvData.personalDetails match {
      case Some(data) => data.nino.nino
      case None =>
        auditService.audit(AuditUtils.buildAuditEvent(pdvData.personalDetails, "FindYourNinoError",
          pdvData.validationStatus, "Empty", pdvData.id, None, Some("/checkDetails"), None, Some("No Personal Details found in PDV data, likely validation failed")))
        logger.debug("No Personal Details found in PDV data, likely validation failed")
        ""
    })).value
  }

  def getIndividualDetails(nino: IndividualDetailsNino
                          )(implicit ec: ExecutionContext, hc: HeaderCarrier): IndividualDetailsResponseEnvelope[IndividualDetails] = {
    implicit val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCrypto(appConfig.cacheSecretKey)
    implicit val correlationId: CorrelationId = CorrelationId(UUID.randomUUID())
    IndividualDetailsResponseEnvelope.fromEitherF(individualDetailsConnector.getIndividualDetails(nino, ResolveMerge('Y')).value)
  }

  /**
   * This method will create a PDV data row from the PDV Match data and return the rowId and PDV data
   *
   * @param validationId
   * @param hc
   * @returns Future (rowdId and PDV data)
   */
  def getPDVData(body: PDVRequest)(implicit hc: HeaderCarrier): Future[PDVResponseData] = {
    for {
      pdvData <- personalDetailsValidationService.createPDVDataFromPDVMatch(body)
      //pdvData <- personalDetailsValidationService.getPersonalDetailsValidationByValidationId(pdvValidationId)
    } yield pdvData match {
      case data @ PDVResponseData(_, _, _, _, _, _, _) => data //returning a tuple of rowId and PDV data
      case _ => {
        auditService.audit(AuditUtils.buildAuditEvent(None, "FindYourNinoError",
          "failure", "Empty", "", None, Some("/checkDetails"), None, Some("No PDV data found")))
        throw new Exception("No PDV data found")
      }
    }
  }

  def getNPSPostCode(idData: IndividualDetails): String =
    getAddressTypeResidential(idData.addressList).addressPostcode.map(_.value).getOrElse("")


  def checkConditions(idData: IndividualDetails, pdvPostCode: String): (Boolean, String) = {
    var reason = ""

    if (!idData.accountStatusType.exists(_.equals(FullLive))) {
      reason += "AccountStatusType is not FullLive;"
    }
    if (idData.crnIndicator.equals(True)) {
      reason += "CRN;"
    }
    if (!getAddressTypeResidential(idData.addressList).addressStatus.exists(_.equals(NotDlo))) {
      reason += "ResidentialAddressStatus is Dlo or Nfa;"
    }
    if (!(getAddressTypeResidential(idData.addressList).addressPostcode.exists(_.value.equals(pdvPostCode)))) {
      reason += "ResidentialPostcode is not equal to PDVPostcode;"
    }

    val status = {
      idData.accountStatusType.exists(_.equals(FullLive)) &&
        idData.crnIndicator.equals(False) &&
        getAddressTypeResidential(idData.addressList).addressStatus.exists(_.equals(NotDlo)) &&
        getAddressTypeResidential(idData.addressList).addressPostcode.exists(_.value.equals(pdvPostCode))
    }

    (status, reason)

  }

  def getAddressTypeResidential(addressList: AddressList): Address = {
    val residentialAddress = addressList.getAddress.filter(_.addressType.equals(ResidentialAddress))
    residentialAddress.head
  }

}
