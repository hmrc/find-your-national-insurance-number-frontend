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
import models.individualdetails.AddressType._
import models.individualdetails.CrnIndicator._
import models.individualdetails.{Address, AddressList, IndividualDetails, ResolveMerge}
import models.pdv.{PDVRequest, PDVResponseData}
import models.{CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope, Mode}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AuditService, PersonalDetailsValidationService}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.AuditUtils

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request => {
      val result: Try[Future[Result]] = Try {

        lazy val toAuthCredentialId: Option[Credentials] => Future[Option[String]] =
          (credentials: Option[Credentials]) => Future.successful(credentials.map(_.providerId))

        val processData = for {
          credentialId <- authorised().retrieve(credentials)(toAuthCredentialId).recover { case _ => None }
          pdvRequest = PDVRequest(credentialId.getOrElse(""), request.session.data.getOrElse("sessionId", ""))
          pdvData <- getPDVData(pdvRequest)
          idData <- getIdData(pdvData)
        } yield (pdvData, idData) match {
          case (pdvData, Left(_)) =>
            auditService.audit(AuditUtils.buildAuditEvent(pdvData.personalDetails, "StartFindYourNino",
              pdvData.validationStatus, "", None, None, None, None, None, None))
            Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
          case (pdvData, Right(idData)) => {
            auditService.audit(AuditUtils.buildAuditEvent(pdvData.personalDetails, "StartFindYourNino",
              pdvData.validationStatus, idData.crnIndicator.asString, None, None, None, None, None, None))

            val NPSChecks = checkConditions(idData)

            if (!NPSChecks._1 || pdvData.validationStatus.equals("failure")) {
              Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
            } else {
              personalDetailsValidationService.updatePDVDataRowWithValidationStatus(pdvData.id, NPSChecks._1, NPSChecks._2)
              val idPostCode = getNPSPostCode(idData)
              if (pdvData.getPostCode.nonEmpty) {
                if (idPostCode.equals(pdvData.getPostCode)) {
                  Redirect(routes.ValidDataNINOHelpController.onPageLoad(mode = mode))
                } else {
                  Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
                }
              } else {
                personalDetailsValidationService.updatePDVDataRowWithNPSPostCode(pdvData.getNino, idPostCode)
                Redirect(routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(mode = mode))
              }
            }
          }
          case _ => {
            logger.debug("No Personal Details found in PDV data, likely validation failed")
            Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
          }
        }

        processData.recover {
          case ex: Exception =>
            logger.error(s"An error occurred, redirecting....: ${ex.getMessage}")
            Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
        }
      }

      result match {
        case Success(res) => res
        case Failure(ex) =>
          logger.error(s"An error occurred, redirecting.... ${ex.getMessage}")
          Future(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
      }
    }
  }

  def getIdData(pdvData: PDVResponseData)(implicit hc: HeaderCarrier): Future[Either[IndividualDetailsError, IndividualDetails]] = {
    val idData = getIndividualDetails(IndividualDetailsNino(pdvData.personalDetails match {
      case Some(data) => data.nino.nino
      case None =>
        logger.debug("No Personal Details found in PDV data, likely validation failed")
        ""
    })).value
    idData.recover {
      case ex: HttpException =>
        auditService.audit(AuditUtils.buildAuditEvent(pdvData.personalDetails, "FindYourNinoError",
          pdvData.validationStatus, "", None, None, None, Some("/checkDetails"), Some(ex.responseCode.toString), Some(ex.message)))
        logger.debug(s"Failed to retrieve Individual Details data, status: ${ex.responseCode}")
        throw ex
      case ex =>
        throw ex
    }
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
    val p = for {
      pdvData <- personalDetailsValidationService.createPDVDataFromPDVMatch(body)
    } yield pdvData match {
      case data@PDVResponseData(_, _, _, _, _, _, _, _) => data //returning a tuple of rowId and PDV data
      case _ => {
        throw new Exception("No PDV data found")
      }
    }
    p.recover {
      case ex: HttpException =>
        auditService.audit(AuditUtils.buildAuditEvent(None, "FindYourNinoError",
          "", "", None, None, None, Some("/checkDetails"), Some(ex.responseCode.toString), Some(ex.message)))
        logger.debug(ex.getMessage)
        throw ex
    }
  }

  def getNPSPostCode(idData: IndividualDetails): String =
    getAddressTypeResidential(idData.addressList).addressPostcode.map(_.value).getOrElse("")

  def getAddressTypeResidential(addressList: AddressList): Address = {
    val residentialAddress = addressList.getAddress.filter(_.addressType.equals(ResidentialAddress))
    residentialAddress.head
  }

  def checkConditions(idData: IndividualDetails): (Boolean, String) = {
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

    val status = {
      idData.accountStatusType.exists(_.equals(FullLive)) &&
        idData.crnIndicator.equals(False) &&
        getAddressTypeResidential(idData.addressList).addressStatus.exists(_.equals(NotDlo))

    }

    (status, reason)
  }

}
