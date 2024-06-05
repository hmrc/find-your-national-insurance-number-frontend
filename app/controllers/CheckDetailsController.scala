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
import models.errors.ConnectorError
import models.pdv._
import models.{Mode, UserAnswers}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
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
                                        val authConnector: AuthConnector
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with AuthorisedFunctions with I18nSupport with Logging {

  def onPageLoad(origin: Option[String], mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request => {
        auditService.start()

        origin.map(_.toUpperCase) match {
          case Some(PDVOrigin) | Some(IVOrigin) | Some(FMNOrigin) =>
            cacheOrigin(request.userAnswers, origin)

            val pdvRequest = PDVRequest(
              request.credId.getOrElse(EmptyString),
              request.session.data.getOrElse("sessionId", EmptyString)
            )

            // TODO correctly handle PDV errors
            personalDetailsValidationService.getPDVData(pdvRequest) flatMap  {
              case PDVSuccessResponse(pdvResponseData) =>
                val sessionWithNINO = request.session + ("nino" -> pdvResponseData.getNino)
                checkMatching(pdvResponseData, mode, sessionWithNINO, origin)
              case PDVBadRequestResponse(r) => ???
              case PDVNotFoundResponse(r) =>
                auditService.findYourNinoPDVNoMatchData(origin)
                ???
              case PDVUnexpectedResponse(r) => ???
              case PDVErrorResponse(cause) => ???
            }
          case _ =>
            logger.error(s"Invalid origin: $origin")
            Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
        }
      }
    }

  private def cacheOrigin(userAnswers: UserAnswers, origin: Option[String]): Future[UserAnswers] = {
    for {
      updatedAnswers <- Future.fromTry(userAnswers.set(OriginCacheable, origin.getOrElse("None")))
      _ <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers
  }

  private def checkMatching(pdvData: PDVResponseData,
                            mode: Mode,
                            sessionWithNINO: Session,
                            origin: Option[String])(implicit headerCarrier: HeaderCarrier): Future[Result] = {

    if (pdvData.validationStatus.toLowerCase.equals("success")) {
      checkIndividualsDetails(pdvData, mode, sessionWithNINO, origin)
    } else {
      logger.warn(s"PDV matched failed: ${pdvData.validationStatus}")
      auditService.findYourNinoPDVMatchFailed(pdvData, origin)
      Future.successful(redirectToInvalidDataNINOHelpController(mode, sessionWithNINO))
    }
  }

  private def checkIndividualsDetails(pdvData: PDVResponseData,
                                      mode: Mode,
                                      sessionWithNINO: Session,
                                      origin: Option[String])
                                     (implicit headerCarrier: HeaderCarrier): Future[Result] = {
    individualDetailsService.getIdDataNew(pdvData) map {
      case Right(idData) =>
        auditService.findYourNinoPDVMatched(pdvData, origin, Some(idData))

        // TODO - wait for this future to complete
        individualDetailsService.createIndividualDetailsData(sessionWithNINO.data.getOrElse("sessionId", EmptyString), idData)

        val api1694Checks = checkDetailsService.checkConditions(idData)

        // TODO - wait for this future to complete
        personalDetailsValidationService.updatePDVDataRowWithValidCustomer(pdvData.getNino, api1694Checks._1, api1694Checks._2)
        if (api1694Checks._1) {
          val idPostCode = individualDetailsService.getNPSPostCode(idData)
          if (pdvData.getPostCode.nonEmpty) {
            if (comparePostCode(idPostCode, pdvData.getPostCode)) {
              logger.info(s"PDV and API 1694 postcodes matched")
              Redirect(routes.ValidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
            } else {
              logger.warn(s"PDV and API 1694 postcodes not matched")
              redirectToInvalidDataNINOHelpController(mode, sessionWithNINO)
            }
          } else {
            personalDetailsValidationService.updatePDVDataRowWithNPSPostCode(pdvData.getNino, idPostCode)
            Redirect(routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
          }
        } else {
          logger.warn(s"API 1694 checks failed: ${api1694Checks._2}")
          redirectToInvalidDataNINOHelpController(mode, sessionWithNINO)
        }
      case Left(error) =>
        // TODO correctly handle 1694 errors
        auditService.findYourNinoPDVMatched(pdvData, origin, None)

        val errorStatusCode: Option[String] = error match {
          case conError: ConnectorError => Some(conError.statusCode.toString)
          case _ => None
        }

        auditService.findYourNinoIdDataError(pdvData, errorStatusCode, error, origin)
        logger.warn(s"Failed to retrieve Individual Details data: ${error.errorMessage}")
        redirectToInvalidDataNINOHelpController(mode, sessionWithNINO)

    }
  }

  private def redirectToInvalidDataNINOHelpController(mode: Mode, sessionWithNINO: Session): Result = {
    Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)).withSession(sessionWithNINO)
  }

}
