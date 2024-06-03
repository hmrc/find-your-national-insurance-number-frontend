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
import models.individualdetails.IndividualDetails
import models.pdv.{PDVNotFoundResponse, PDVRequest, PDVResponse, PDVResponseData, PDVSuccessResponse}
import models.requests.DataRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc._
import repositories.SessionRepository
import services.{AuditService, CheckDetailsService, IndividualDetailsService, PersonalDetailsValidationService}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.FMNConstants.{EmptyString, FMNOrigin, IVOrigin, PDVOrigin}
import util.FMNHelper.comparePostCode

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
                                        checkDetailsService: CheckDetailsService,
                                        individualDetailsService: IndividualDetailsService,
                                        sessionRepository: SessionRepository,
                                        val controllerComponents: MessagesControllerComponents,
                                        val authConnector: AuthConnector,
                                        errorHandler: ErrorHandler
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with AuthorisedFunctions with I18nSupport with Logging {

  def onPageLoad(origin: Option[String], mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request => {
        auditService.start()

        origin.map(_.toUpperCase) match {
          case Some(PDVOrigin) | Some(IVOrigin) | Some(FMNOrigin) =>
            validOriginJourney(origin, mode)
          case _ =>
            logger.error(s"Invalid origin: $origin")
            Future(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
        }
      }
    }

  private def validOriginJourney(origin: Option[String],
                                 mode: Mode
                                )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[Result] = {

    logger.info(s"Valid origin: $origin")

    val pdvRequest = PDVRequest(
      request.credId.getOrElse(EmptyString),
      request.session.data.getOrElse("sessionId", EmptyString)
    )

    val result: Try[Future[Result]] = Try {
      val processData = for {
        updatedAnswers <- Future.fromTry(request.userAnswers.set(OriginCacheable, origin.getOrElse("None")))
        _ <- sessionRepository.set(updatedAnswers)
        pdvData <- personalDetailsValidationService.getPDVData(pdvRequest)
        idData  <- individualDetailsService.getIdData(pdvData)
        sessionWithNINO = request.session + ("nino" -> getNinoFromPDVResponse(pdvData))
      } yield (pdvData, idData, sessionWithNINO) match {
        case (pdvData, Left(idData), sessionWithNINO)  => checkDetailsFailureJourney(pdvData, idData, mode, sessionWithNINO, origin)
        case (pdvData, Right(idData), sessionWithNINO) => checkDetailsSuccessJourney(pdvData, idData, mode, sessionWithNINO, origin)
        case _                                         => checkDetailsMatchingFailedWithUnknownIssue(mode)
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

  private def checkDetailsFailureJourney(pdvResponse: PDVResponse,
                                         idDataError: IndividualDetailsError,
                                         mode: Mode,
                                         sessionWithNINO: Session,
                                         origin: Option[String])(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Result =
    pdvResponse match {
      case _ @ PDVSuccessResponse(pdvData: PDVResponseData) =>
        handleCheckDetailsFailureJourney(idDataError, mode, sessionWithNINO, origin, pdvData)
      case _ @ PDVNotFoundResponse(_) =>
        auditService.findYourNinoPDVNoMatchData(origin)
        logger.warn(s"PDV data not found.")
        redirectToInvalidDataNINOHelpController(mode, sessionWithNINO)
      case _ =>
        checkDetailsMatchingFailedWithUnknownIssue(mode)
    }

  // TODO Will remove it after review
  //      private def handleCheckDetailsFailureJourney(idDataError: IndividualDetailsError,
  //                                               mode: Mode,
  //                                               sessionWithNINO: Session,
  //                                               origin: Option[String],
  //                                               pdvData: PDVResponseData)(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Result = {
  //    if (pdvData.validationStatus.equals("failure")) {
  //      logger.warn(s"PDV matched failed: ${pdvData.validationStatus}")
  //      auditService.findYourNinoPDVMatchFailed(pdvData, origin)
  //      Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
  //    } else if (pdvData.validationStatus.equals("success")) {
  //      auditService.findYourNinoPDVMatched(pdvData, origin, None)
  //
  //      val errorStatusCode: Option[String] = idDataError match {
  //        case conError: ConnectorError => Some(conError.statusCode.toString)
  //        case _ => None
  //      }
  //
  //      auditService.findYourNinoIdDataError(pdvData, errorStatusCode, idDataError, origin)
  //      logger.warn(s"Failed to retrieve Individual Details data: ${idDataError.errorMessage}")
  //      // TODO review FailedDependency or use other status here???
  //      FailedDependency(errorHandler.standardErrorTemplate(
  //          Messages("global.error.InternalServerError500.title"),
  //          Messages("global.error.InternalServerError500.heading"),
  //          Messages("global.error.InternalServerError500.message")
  //      )(request))
  //    } else {
  //      checkDetailsMatchingFailedWithUnknownIssue(mode)
  //    }
  //  }

  private def handleCheckDetailsFailureJourney(idDataError: IndividualDetailsError,
                                               mode: Mode,
                                               sessionWithNINO: Session,
                                               origin: Option[String],
                                               pdvData: PDVResponseData
                                              )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Result =
    pdvData.validationStatus match {
      case "failure" =>
        logger.warn(s"PDV matched failed: ${pdvData.validationStatus}")
        auditService.findYourNinoPDVMatchFailed(pdvData, origin)
        redirectToInvalidDataNINOHelpController(mode, sessionWithNINO)
      case "success" =>
        handleSuccessValidationStatus(idDataError, origin, pdvData)
      case _ =>
        checkDetailsMatchingFailedWithUnknownIssue(mode)
    }

  private def handleSuccessValidationStatus(idDataError: IndividualDetailsError,
                                            origin: Option[String],
                                            pdvData: PDVResponseData
                                           )(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Result = {
    auditService.findYourNinoPDVMatched(pdvData, origin, None)

    val errorStatusCode: Option[String] = idDataError match {
      case conError: ConnectorError => Some(conError.statusCode.toString)
      case _ => None
    }

    auditService.findYourNinoIdDataError(pdvData, errorStatusCode, idDataError, origin)
    logger.warn(s"Failed to retrieve Individual Details data: ${idDataError.errorMessage}")
    // TODO review FailedDependency or use other status here???
    FailedDependency(errorHandler.standardErrorTemplate(
      Messages("global.error.InternalServerError500.title"),
      Messages("global.error.InternalServerError500.heading"),
      Messages("global.error.InternalServerError500.message")
    )(request))
  }

  private def checkDetailsSuccessJourney(pdvResponse: PDVResponse,
                                         idData: IndividualDetails,
                                         mode: Mode,
                                         sessionWithNINO: Session,
                                         origin: Option[String])(implicit  headerCarrier: HeaderCarrier): Result = {

    pdvResponse match {
      case _ @ PDVSuccessResponse(pdvData: PDVResponseData) =>
        if (pdvData.validationStatus.equals("success")) {
          checkDetailsMatchingSuccess(pdvData, idData, mode, sessionWithNINO, origin)
        } else {
          logger.warn(s"PDV matched failed: ${pdvData.validationStatus}")
          auditService.findYourNinoPDVMatchFailed(pdvData, origin)
          redirectToInvalidDataNINOHelpController(mode, sessionWithNINO)
        }
      case _ @ PDVNotFoundResponse(_) =>
        auditService.findYourNinoPDVNoMatchData(origin)
        logger.warn(s"PDV data not found.")
        redirectToInvalidDataNINOHelpController(mode, sessionWithNINO)
      case _ =>
        checkDetailsMatchingFailedWithUnknownIssue(mode)
    }
  }

  private def checkDetailsMatchingSuccess(pdvData: PDVResponseData,
                                          idData: IndividualDetails,
                                          mode: Mode,
                                          sessionWithNINO: Session,
                                          origin: Option[String])(implicit  headerCarrier: HeaderCarrier): Result = {
    auditService.findYourNinoPDVMatched(pdvData, origin, Some(idData))

    individualDetailsService.createIndividualDetailsData(sessionWithNINO.data.getOrElse("sessionId", EmptyString), idData)

    val api1694Checks = checkDetailsService.checkConditions(idData)
    personalDetailsValidationService.updatePDVDataRowWithValidCustomer(pdvData.getNino, api1694Checks._1, api1694Checks._2)
    if (api1694Checks._1) {
      val idPostCode = individualDetailsService.getNPSPostCode(idData)
      if (pdvData.getPostCode.nonEmpty) {
        // Matched with PostCode
        if (comparePostCode(idPostCode, pdvData.getPostCode)) {
          logger.info(s"PDV and API 1694 postcodes matched")
          Redirect(routes.ValidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
        } else {
          logger.warn(s"PDV and API 1694 postcodes not matched")
          redirectToInvalidDataNINOHelpController(mode, sessionWithNINO)
        }
      } else { // Matched with NINO
        personalDetailsValidationService.updatePDVDataRowWithNPSPostCode(pdvData.getNino, idPostCode)
        Redirect(routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
      }
    } else {
      logger.warn(s"API 1694 checks failed: ${api1694Checks._2}")
      redirectToInvalidDataNINOHelpController(mode, sessionWithNINO)
    }
  }

  private def checkDetailsMatchingFailedWithUnknownIssue(mode: Mode): Result = {
    logger.warn("No Personal Details found in PDV data, likely validation failed")
    Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
  }

  private def getNinoFromPDVResponse(pdvData: PDVResponse): String = pdvData match {
      case PDVSuccessResponse(data: PDVResponseData) => data.getNino
      case _ => EmptyString
    }

  private def redirectToInvalidDataNINOHelpController(mode: Mode, sessionWithNINO: Session): Result = {
    Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
  }

}
