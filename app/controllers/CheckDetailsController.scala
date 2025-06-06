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

import controllers.actions.{DataRetrievalAction, IdentifierAction}
import handlers.ErrorHandler
import models.errors.{ConnectorError, IndividualDetailsError}
import models.pdv._
import models.requests.DataRequest
import models.{Mode, OriginType}
import org.apache.commons.lang3.StringUtils
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{AuditService, CheckDetailsService, IndividualDetailsService, PersonalDetailsValidationService}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.FMNHelper.comparePostCode

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  personalDetailsValidationService: PersonalDetailsValidationService,
  auditService: AuditService,
  checkDetailsService: CheckDetailsService,
  individualDetailsService: IndividualDetailsService,
  val controllerComponents: MessagesControllerComponents,
  val authConnector: AuthConnector,
  errorHandler: ErrorHandler
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with AuthorisedFunctions
    with I18nSupport
    with Logging {

  def onPageLoad(optOrigin: Option[OriginType], mode: Mode): Action[AnyContent] =
    (identify andThen getData(optOrigin)).async { implicit request =>
      auditService.start()
      optOrigin match {
        case Some(_) => pdvCheck(mode, optOrigin)
        case _       =>
          logger.error(s"Missing valid origin: $optOrigin")
          Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
      }
    }

  private def pdvCheck(mode: Mode, origin: Option[OriginType])(implicit
    hc: HeaderCarrier,
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    val pdvRequest = PDVRequest(
      request.credId.getOrElse(StringUtils.EMPTY),
      request.session.data.getOrElse("sessionId", StringUtils.EMPTY)
    )
    personalDetailsValidationService.getPDVData(pdvRequest) flatMap {
      case PDVSuccessResponse(pdvResponseData) =>
        val sessionWithNINO = request.session + ("nino" -> pdvResponseData.getNino)
        pdvResponseData.validationStatus match {
          case ValidationStatus.Success =>
            individualsDetailsChecks(pdvResponseData, mode, sessionWithNINO, origin)
          case _                        =>
            logger.info(s"PDV matched failed: ${pdvResponseData.validationStatus}")
            auditService.findYourNinoPDVMatchFailed(pdvResponseData, origin)
            Future.successful(
              Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
            )
        }
      case PDVNotFoundResponse(r)              =>
        logger.info(s"No PDV data found: ${r.status}")
        if (!r.body.contains("No association found") && !r.body.contains("No record found using validation ID")) {
          auditService.findYourNinoGetPdvDataHttpError(r.status.toString, r.body, request.origin)
        } else {
          auditService.findYourNinoPDVNoMatchData(origin)
        }
        Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
      case PDVBadRequestResponse(r)            =>
        logger.error(s"Bad request: ${r.status}")
        auditService.findYourNinoGetPdvDataHttpError(r.status.toString, r.body, request.origin)
        errorHandler.standardErrorTemplate().map(BadRequest(_))
      case PDVUnexpectedResponse(r)            =>
        logger.error(s"Unexpected response: ${r.status}")
        auditService.findYourNinoGetPdvDataHttpError(r.status.toString, r.body, request.origin)
        errorHandler.standardErrorTemplate().map(InternalServerError(_))
      case PDVErrorResponse(cause)             =>
        logger.error(s"Error response: $cause")
        auditService.findYourNinoGetPdvDataHttpError(cause.status.toString, cause.body, request.origin)
        errorHandler.standardErrorTemplate().map(InternalServerError(_))
    }
  }

  private def individualsDetailsChecks(
    pdvData: PDVResponseData,
    mode: Mode,
    sessionWithNINO: Session,
    origin: Option[OriginType]
  )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Result] =
    individualDetailsService.getIdData(pdvData) flatMap {
      case Right(idData) =>
        auditService.findYourNinoPDVMatched(pdvData, origin, Some(idData))
        val sessionId = sessionWithNINO.data.getOrElse("sessionId", StringUtils.EMPTY)

        val checksf: Future[(Boolean, String)] = for {
          _ <- individualDetailsService.createIndividualDetailsData(sessionId, idData)
          c  = checkDetailsService.checkConditions(idData)
          _  = personalDetailsValidationService.updatePDVDataRowWithValidCustomer(pdvData.getNino, c._1, c._2)
        } yield c

        checksf.flatMap { api1694Checks =>
          if (api1694Checks._1) {
            val idPostCode = individualDetailsService.getNPSPostCode(idData)
            if (pdvData.getPostCode.nonEmpty) {
              if (comparePostCode(idPostCode, pdvData.getPostCode)) {
                logger.info(s"PDV and API 1694 postcodes matched")
                Future.successful(
                  Redirect(routes.ValidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
                )
              } else {
                logger.info(s"PDV and API 1694 postcodes not matched")
                Future.successful(
                  Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
                )
              }
            } else {
              personalDetailsValidationService.updatePDVDataRowWithNPSPostCode(pdvData.getNino, idPostCode)
              Future.successful(
                Redirect(routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(mode = mode))
                  .withSession(sessionWithNINO)
              )
            }
          } else {
            logger.info(s"API 1694 checks failed: ${api1694Checks._2}")
            Future.successful(
              Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
            )
          }
        }
      case Left(error)   =>
        auditService.findYourNinoPDVMatched(pdvData, origin, None)
        individualsDetailsError(error, pdvData, origin)
    }

  private def individualsDetailsError(
    error: IndividualDetailsError,
    PDVResponseData: PDVResponseData,
    origin: Option[OriginType]
  )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Result] =
    error match {
      case conError: ConnectorError =>
        conError.statusCode match {
          case INTERNAL_SERVER_ERROR | BAD_GATEWAY | SERVICE_UNAVAILABLE =>
            auditService.findYourNinoIdDataError(PDVResponseData, Some(conError.statusCode.toString), error, origin)
            logger.error(s"Failed to retrieve Individual Details data: ${error.errorMessage}")
            errorHandler.standardErrorTemplate().map(InternalServerError(_))
          case BAD_REQUEST                                               =>
            auditService.findYourNinoIdDataError(PDVResponseData, Some(conError.statusCode.toString), error, origin)
            logger.error(s"Failed to retrieve Individual Details data: ${error.errorMessage}")
            errorHandler.standardErrorTemplate().map(BadRequest(_))
          case UNPROCESSABLE_ENTITY                                      =>
            auditService.findYourNinoIdDataError(PDVResponseData, Some(conError.statusCode.toString), error, origin)
            Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad()))
          case code                                                      =>
            auditService.findYourNinoIdDataError(PDVResponseData, Some(code.toString), error, origin)
            logger.error(s"Failed to retrieve Individual Details data: ${error.errorMessage}")
            errorHandler.standardErrorTemplate().map(InternalServerError(_))
        }
      case _                        =>
        auditService.findYourNinoIdDataError(PDVResponseData, None, error, origin)
        logger.error(s"Failed to retrieve Individual Details data: ${error.errorMessage}")
        errorHandler.standardErrorTemplate().map(InternalServerError(_))
    }

}
