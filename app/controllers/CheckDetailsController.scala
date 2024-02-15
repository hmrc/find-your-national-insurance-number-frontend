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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.Mode
import models.errors.{ConnectorError, IndividualDetailsError}
import models.individualdetails.IndividualDetails
import models.pdv.{PDVRequest, PDVResponseData}
import models.requests.DataRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{AuditService, CheckDetailsService, PersonalDetailsValidationService}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.FMNConstants.{EmptyString, IVOrigin, PDVOrigin}
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
                                        val controllerComponents: MessagesControllerComponents,
                                        val authConnector: AuthConnector
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with AuthorisedFunctions with I18nSupport with Logging {

  def onPageLoad(origin: Option[String], mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request => {
        auditService.start(origin)
        origin.map(_.toUpperCase) match {
          case Some(PDVOrigin) | Some(IVOrigin) =>
            validOriginJourney(origin, request, mode)
          case _ =>
            logger.error(s"Invalid origin: $origin")
            Future(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
        }
      }
    }

  private def validOriginJourney(origin: Option[String], request: DataRequest[AnyContent], mode: Mode)
                                (implicit headerCarrier: HeaderCarrier): Future[Result] = {
    logger.info(s"Valid origin: $origin")

    val pdvRequest = PDVRequest(
      request.credId.getOrElse(EmptyString),
      request.session.data.getOrElse("sessionId", EmptyString)
    )

    println("ACHI: " + pdvRequest)

    val result: Try[Future[Result]] = Try {
      val processData = for {
        pdvData <- checkDetailsService.getPDVData(pdvRequest)
        idData  <- checkDetailsService.getIdData(pdvData)
        sessionWithNINO = request.session + ("nino" -> pdvData.getNino)
      } yield (pdvData, idData, sessionWithNINO) match {
        case (pdvData, Left(idData), sessionWithNINO)  => checkDetailsFailureJourney(pdvData, idData, mode, sessionWithNINO, origin)
        case (pdvData, Right(idData), sessionWithNINO) => checkDetailsSuccessJourney(pdvData, idData, mode, sessionWithNINO, origin)
        case _                                         => checkDetailsMatchingFailedWithUnknownIssue(mode)
      }
      processData.recover {
        case ex: Exception =>
          println("ACHI: processData.recover")
          logger.error(s"An error occurred in process data: ${ex.getMessage}")
          Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
      }
    }
    result match {
      case Success(res) => res
      case Failure(ex) =>
        println("ACHI: result match")
        logger.error(s"An error occurred, redirecting: ${ex.getMessage}")
        Future(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
    }
  }

  private def checkDetailsFailureJourney(pdvData: PDVResponseData, idDataError: IndividualDetailsError,
                                         mode: Mode, sessionWithNINO: Session, origin: Option[String])
                                        (implicit headerCarrier: HeaderCarrier): Result = {
    if (pdvData.validationStatus.equals("failure")) {
      logger.warn(s"PDV matched failed: ${pdvData.validationStatus}")

      println("ACHI: checkDetailsFailureJourney")
      auditService.findYourNinoPDVMatchFailed(pdvData, origin)
      Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
    } else {
      val errorStatusCode: Option[String] = idDataError match {
        case conError: ConnectorError => Some(conError.statusCode.toString)
        case _ => None
      }

      auditService.findYourNinoIdDataError(pdvData, errorStatusCode, idDataError, origin)
      logger.warn(s"Failed to retrieve Individual Details data: ${idDataError.errorMessage}")
      Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
    }
  }

  private def checkDetailsSuccessJourney(pdvData: PDVResponseData, idData: IndividualDetails,
                                         mode: Mode, sessionWithNINO: Session, origin: Option[String])
                                        (implicit  headerCarrier: HeaderCarrier): Result = {
    auditService.findYourNinoPDVMatched(pdvData, origin, idData)

    val api1694Checks = checkDetailsService.checkConditions(idData)
    personalDetailsValidationService.updatePDVDataRowWithValidationStatus(pdvData.getNino, api1694Checks._1, api1694Checks._2)

    if (api1694Checks._1) {
      val idPostCode = checkDetailsService.getNPSPostCode(idData)
      if (pdvData.getPostCode.nonEmpty) {
        // Matched with PostCode
        if (comparePostCode(idPostCode, pdvData.getPostCode)) {
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
  }

  private def checkDetailsMatchingFailedWithUnknownIssue(mode: Mode): Result = {
    logger.warn("No Personal Details found in PDV data, likely validation failed")
    Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
  }
}
