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

import cacheables.OriginCacheable
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import handlers.ErrorHandler
import models.Mode
import models.errors.{ConnectorError, IndividualDetailsError}
import models.pdv._
import models.requests.DataRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{AuditService, CheckDetailsService, IndividualDetailsService, PersonalDetailsValidationService}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.FMNConstants.{EmptyString, FMNOrigin, IVOrigin, PDVOrigin}
import util.FMNHelper.comparePostCode

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckDetailsController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        personalDetailsValidationService: PersonalDetailsValidationService,
                                        auditService: AuditService,
                                        checkDetailsService: CheckDetailsService,
                                        individualDetailsService: IndividualDetailsService,
                                        val controllerComponents: MessagesControllerComponents,
                                        val authConnector: AuthConnector,
                                        errorHandler: ErrorHandler,
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with AuthorisedFunctions with I18nSupport with Logging {

  def onPageLoad(origin: Option[String], mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request => {
        auditService.start()
        origin.map(_.toUpperCase) match {
          case Some(PDVOrigin) | Some(IVOrigin) | Some(FMNOrigin) =>
            individualDetailsService.cacheOrigin(request.userAnswers, origin)
            pdvCheck(mode, origin)
          case _ =>
            logger.error(s"Invalid origin: $origin")
            Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
        }
      }
    }

  private def pdvCheck(mode: Mode,
                       origin: Option[String])
                      (implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Result] = {
    val pdvRequest = PDVRequest(
      request.credId.getOrElse(EmptyString),
      request.session.data.getOrElse("sessionId", EmptyString)
    )
    personalDetailsValidationService.getPDVData(pdvRequest) flatMap {
      case PDVSuccessResponse(pdvResponseData) =>
        val sessionWithNINO = request.session + ("nino" -> pdvResponseData.getNino)
        pdvResponseData.validationStatus match {
          case ValidationStatus.Success =>
            individualsDetailsChecks(pdvResponseData, mode, sessionWithNINO, origin)
          case _ =>
            logger.info(s"PDV matched failed: ${pdvResponseData.validationStatus}")
            auditService.findYourNinoPDVMatchFailed(pdvResponseData, origin)
            Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO))
        }
      case PDVNotFoundResponse(r) =>
        logger.info(s"No PDV data found: ${r.status}")
        if (!r.body.contains("No association found") && !r.body.contains("No record found using validation ID")) {
          auditService.findYourNinoGetPdvDataHttpError(r.status.toString, r.body, request.userAnswers.get(OriginCacheable))
        } else {
          auditService.findYourNinoPDVNoMatchData(origin)
        }
        Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
      case PDVBadRequestResponse(r) =>
        logger.error(s"Bad request: ${r.status}")
        auditService.findYourNinoGetPdvDataHttpError(r.status.toString, r.body, request.userAnswers.get(OriginCacheable))
        Future.successful(BadRequest(errorHandler.standardErrorTemplate()))
      case PDVUnexpectedResponse(r) =>
        logger.error(s"Unexpected response: ${r.status}")
        auditService.findYourNinoGetPdvDataHttpError(r.status.toString, r.body, request.userAnswers.get(OriginCacheable))
        Future.successful(InternalServerError(errorHandler.standardErrorTemplate()))
      case PDVErrorResponse(cause) =>
        logger.error(s"Error response: $cause")
        auditService.findYourNinoGetPdvDataHttpError(cause.status.toString, cause.body, request.userAnswers.get(OriginCacheable))
        Future.successful(InternalServerError(errorHandler.standardErrorTemplate()))
    }
  }

  private def individualsDetailsChecks(pdvData: PDVResponseData,
                                      mode: Mode,
                                      sessionWithNINO: Session,
                                      origin: Option[String])
                                     (implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Result] = {
    individualDetailsService.getIdData(pdvData) flatMap {
      case Right(idData) =>
        auditService.findYourNinoPDVMatched(pdvData, origin, Some(idData))
        val sessionId = sessionWithNINO.data.getOrElse("sessionId", EmptyString)

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
                Future.successful(Redirect(routes.ValidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO))
              } else {
                logger.info(s"PDV and API 1694 postcodes not matched")
                Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO))
              }
            } else {
              personalDetailsValidationService.updatePDVDataRowWithNPSPostCode(pdvData.getNino, idPostCode)
              Future.successful(Redirect(routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO))
            }
          } else {
            logger.info(s"API 1694 checks failed: ${api1694Checks._2}")
            Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO))
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
                                     (implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Result = {
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
