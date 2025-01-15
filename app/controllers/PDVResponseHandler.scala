/*
 * Copyright 2025 HM Revenue & Customs
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

import cacheables.OriginCacheable
import handlers.ErrorHandler
import models.errors.{ConnectorError, IndividualDetailsError}
import models.pdv._
import models.{Mode, UserAnswers}
import play.api.Logging
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, UNPROCESSABLE_ENTITY}
import play.api.mvc.Results.{BadRequest, InternalServerError, Redirect}
import play.api.mvc.{AnyContent, Result, Results}
import services.{AuditService, CheckDetailsService, IndividualDetailsService, PersonalDetailsValidationService}
import uk.gov.hmrc.http.HeaderCarrier
import util.FMNConstants.EmptyString
import util.FMNHelper.comparePostCode

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PDVResponseHandler @Inject()(
                                    auditService: AuditService,
                                    errorHandler: ErrorHandler,
                                    personalDetailsValidationService: PersonalDetailsValidationService,
                                    checkDetailsService: CheckDetailsService,
                                    individualDetailsService: IndividualDetailsService,
                                  )(implicit ec: ExecutionContext) extends Logging {

  def getNino(pdvResponse: PDVResponse): Option[String] = pdvResponse match {
    case PDVSuccessResponse(pdvResponseData) if pdvResponseData.validationStatus == ValidationStatus.Success =>
      Some(pdvResponseData.getNino)
    case _ =>
      None
  }

  def handlePDVResponse(
                         pdvResponse: PDVResponse,
                         origin: Option[String],
                         userAnswers: UserAnswers,
                         mode: Mode
                       )(implicit hc: HeaderCarrier, request: PDVDataRequestWithUserAnswers[AnyContent]): Future[Result] = {
    pdvResponse match {
      case PDVSuccessResponse(pdvResponseData) =>
        val sessionId = hc.sessionId.map(_.value).getOrElse(EmptyString)
        pdvResponseData.validationStatus match {
          case ValidationStatus.Success =>
            individualsDetailsChecks(pdvResponseData, mode, sessionId, origin)
          case _ =>
            logger.info(s"PDV match failed: ${pdvResponseData.validationStatus}")
            auditService.findYourNinoPDVMatchFailed(pdvResponseData, origin)
            Future.successful(
              Results.Redirect(routes.InvalidDataNINOHelpController.onPageLoad())
            )
        }
      case PDVBadRequestResponse(response) =>
        logger.error(s"Bad request: ${response.body}")
        auditService.findYourNinoGetPdvDataHttpError(response.status.toString, response.body, userAnswers.get(OriginCacheable))
        Future.successful(Results.BadRequest(errorHandler.standardErrorTemplate()))

      case PDVNotFoundResponse(response) =>
        logger.info(s"No PDV data found: ${response.status}")
        if (!response.body.contains("No association found") && !response.body.contains("No record found using validation ID")) {
          auditService.findYourNinoGetPdvDataHttpError(response.status.toString, response.body, userAnswers.get(OriginCacheable))
        } else {
          auditService.findYourNinoPDVNoMatchData(origin)
        }
        Future.successful(Results.Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))

      case PDVUnexpectedResponse(response) =>
        logger.error(s"Unexpected response: ${response.status}")
        auditService.findYourNinoGetPdvDataHttpError(response.status.toString, response.body, userAnswers.get(OriginCacheable))
        Future.successful(Results.InternalServerError(errorHandler.standardErrorTemplate()))

      case PDVErrorResponse(cause) =>
        logger.error(s"Error response: $cause")
        auditService.findYourNinoGetPdvDataHttpError(cause.status.toString, cause.body, userAnswers.get(OriginCacheable))
        Future.successful(Results.InternalServerError(errorHandler.standardErrorTemplate()))
    }
  }

  private def individualsDetailsChecks(pdvData: PDVResponseData,
                                       mode: Mode,
                                       sessionId: String,
                                       origin: Option[String])
                                      (implicit hc: HeaderCarrier, request: PDVDataRequestWithUserAnswers[AnyContent]): Future[Result] = {
    individualDetailsService.getIdData(pdvData) flatMap {
      case Right(idData) =>
        auditService.findYourNinoPDVMatched(pdvData, origin, Some(idData))

        val checksf: Future[(Boolean, String)] = for {
          _ <- individualDetailsService.createIndividualDetailsData(sessionId, idData)
          c = checkDetailsService.checkConditions(idData)
          _ = personalDetailsValidationService.updatePDVDataRowWithValidCustomer(pdvData.getNino, c._1, c._2)
        } yield c

        checksf.flatMap { api1694Checks =>
          if (api1694Checks._1) {
            val idPostCode = individualDetailsService.getNPSPostCode(idData)
            if (pdvData.getPostCode.nonEmpty) {
              if (comparePostCode(idPostCode, pdvData.getPostCode)) {
                logger.info(s"PDV and API 1694 postcodes matched")
                Future.successful(Redirect(routes.ValidDataNINOHelpController.onPageLoad(mode = mode)))
              } else {
                logger.info(s"PDV and API 1694 postcodes not matched")
                Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
              }
            } else {
              personalDetailsValidationService.updatePDVDataRowWithNPSPostCode(pdvData.getNino, idPostCode)
              Future.successful(Redirect(routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(mode = mode)))
            }
          } else {
            logger.info(s"API 1694 checks failed: ${api1694Checks._2}")
            Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
          }
        }
      case Left(error) =>
        auditService.findYourNinoPDVMatched(pdvData, origin, None)
        Future.successful(individualsDetailsError(error, pdvData, origin))
    }
  }

  private def individualsDetailsError(error: IndividualDetailsError,
                                      PDVResponseData: PDVResponseData,
                                      origin: Option[String])
                                     (implicit hc: HeaderCarrier, request: PDVDataRequestWithUserAnswers[AnyContent]): Result = {
    error match {
      case conError: ConnectorError => conError.statusCode match {
        case INTERNAL_SERVER_ERROR | BAD_GATEWAY | SERVICE_UNAVAILABLE =>
          auditService.findYourNinoIdDataError(PDVResponseData, Some(conError.statusCode.toString), error, origin)
          logger.error(s"Failed to retrieve Individual Details data: ${error.errorMessage}")
          InternalServerError(errorHandler.standardErrorTemplate())
        case BAD_REQUEST =>
          auditService.findYourNinoIdDataError(PDVResponseData, Some(conError.statusCode.toString), error, origin)
          logger.error(s"Failed to retrieve Individual Details data: ${error.errorMessage}")
          BadRequest(errorHandler.standardErrorTemplate())
        case UNPROCESSABLE_ENTITY =>
          auditService.findYourNinoIdDataError(PDVResponseData, Some(conError.statusCode.toString), error, origin)
          Redirect(routes.InvalidDataNINOHelpController.onPageLoad())
        case code =>
          auditService.findYourNinoIdDataError(PDVResponseData, Some(code.toString), error, origin)
          logger.error(s"Failed to retrieve Individual Details data: ${error.errorMessage}")
          InternalServerError(errorHandler.standardErrorTemplate())
      }
      case _ =>
        auditService.findYourNinoIdDataError(PDVResponseData, None, error, origin)
        logger.error(s"Failed to retrieve Individual Details data: ${error.errorMessage}")
        InternalServerError(errorHandler.standardErrorTemplate())
    }
  }
}
