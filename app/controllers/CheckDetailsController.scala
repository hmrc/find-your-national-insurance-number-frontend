/*
 * Copyright 2024 HM Revenue & Customs
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
import models.errors.{ConnectorError, IndividualDetailsError}
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
  
  def onPageLoad(origin: Option[String], mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request => {

      origin.map(_.toUpperCase) match {
        case Some("PDV") | Some("IV") => {
          logger.info(s"Valid origin: $origin")

          val pdvRequest = PDVRequest(
            request.credId.getOrElse(""),
            request.session.data.getOrElse("sessionId", "")
          )

          val result: Try[Future[Result]] = Try {
            val processData = for {
              pdvData <- getPDVData(pdvRequest)
              idData <- getIdData(pdvData)
            } yield (pdvData, idData) match {

              case (pdvData, Left(idData)) =>
                if (pdvData.validationStatus.equals("failure")) {
                  auditService.audit(AuditUtils.buildAuditEvent(pdvData.personalDetails, None, "StartFindYourNino",
                    pdvData.validationStatus, "", None, None, None, None, None, None))
                  Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
                } else {

                  val errorStatusCode: Option[String] = idData match {
                    case conError: ConnectorError => Some(conError.statusCode.toString)
                    case _ => None
                  }

                  auditService.audit(AuditUtils.buildAuditEvent(pdvData.personalDetails, None, "FindYourNinoError",
                    pdvData.validationStatus, "", None, None, None, Some("/checkDetails"), errorStatusCode, Some(idData.errorMessage)))
                  logger.warn(s"Failed to retrieve Individual Details data: ${idData.errorMessage}")
                  Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
                }

              case (pdvData, Right(idData)) =>
                auditService.audit(AuditUtils.buildAuditEvent(pdvData.personalDetails, None, "StartFindYourNino",
                  pdvData.validationStatus, idData.crnIndicator.asString, None, None, None, None, None, None))

                val api1694Checks = checkConditions(idData)
                personalDetailsValidationService.updatePDVDataRowWithValidationStatus(pdvData.id, api1694Checks._1, api1694Checks._2)

                val sessionWithNINO = request.session + ("nino" -> pdvData.getNino)

                if (api1694Checks._1) {
                  val idPostCode = getNPSPostCode(idData)
                  if (pdvData.getPostCode.nonEmpty) {
                    // Matched with Postcode
                    if (idPostCode.equals(pdvData.getPostCode)) {
                      logger.info(s"PDV and API 1694 postcodes matched")
                      Redirect(routes.ValidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
                    } else {
                      logger.warn(s"PDV and API 1694 postcodes not matched")
                      Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
                    }
                  } else { // Matched with NINO
                    personalDetailsValidationService.updatePDVDataRowWithNPSPostCode(pdvData.getNino, idPostCode)
                    Redirect(routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
                  }
                } else {
                  logger.warn(s"API 1694 checks failed: ${api1694Checks._2}")
                  Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
                }

              case _ =>
                logger.warn("No Personal Details found in PDV data, likely validation failed")
                Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))

            }

            processData.recover {
              case ex: Exception =>
                logger.error(s"An error occurred in process data: ${ex.getMessage}")
                Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
            }
          }

          result match {
            case Success(res) => res
            case Failure(ex) =>
              logger.error(s"An error occurred, redirecting: ${ex.getMessage}")
              Future(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
          }
        }

        case _ =>
          logger.error(s"Invalid origin: $origin")
          Future(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))

      }
    }
  }

  private def getIdData(pdvData: PDVResponseData)(implicit hc: HeaderCarrier): Future[Either[IndividualDetailsError, IndividualDetails]] = {
    getIndividualDetails(IndividualDetailsNino(pdvData.personalDetails match {
      case Some(data) => data.nino.nino
      case None =>
        logger.warn("No Personal Details found in PDV data, likely validation failed")
        ""
    })).value
  }

  private def getIndividualDetails(nino: IndividualDetailsNino
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
  private def getPDVData(body: PDVRequest)(implicit hc: HeaderCarrier): Future[PDVResponseData] = {
    val p = for {
      pdvData <- personalDetailsValidationService.createPDVDataFromPDVMatch(body)
    } yield pdvData match {
      case data: PDVResponseData => data //returning a tuple of rowId and PDV data
      case _ => throw new Exception("No PDV data found")
    }
    p.recover {
      case ex: HttpException =>
        auditService.audit(AuditUtils.buildAuditEvent(None, None, "FindYourNinoError",
          "", "", None, None, None, Some("/checkDetails"), Some(ex.responseCode.toString), Some(ex.message)))
        logger.debug(ex.getMessage)
        throw ex
    }
  }

  private def getNPSPostCode(idData: IndividualDetails): String =
    getAddressTypeResidential(idData.addressList).addressPostcode.map(_.value).getOrElse("")

  private def getAddressTypeResidential(addressList: AddressList): Address = {
    val residentialAddress = addressList.getAddress.filter(_.addressType.equals(ResidentialAddress))
    residentialAddress.head
  }

  private def checkConditions(idData: IndividualDetails): (Boolean, String) = {
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
