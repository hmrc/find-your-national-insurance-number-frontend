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

import controllers.actions._
import forms.ConfirmYourPostcodeFormProvider
import models.nps.{LetterIssuedResponse, NPSFMNRequest, RLSDLONFAResponse, TechnicalIssueResponse}
import models.pdv.{PDVResponseData, PersonalDetails}

import javax.inject.Inject
import models.{Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.ConfirmYourPostcodePage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{AuditService, NPSFMNService, PersonalDetailsValidationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ConfirmYourPostcodeView
import util.AuditUtils

import scala.concurrent.{ExecutionContext, Future}

class ConfirmYourPostcodeController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: ConfirmYourPostcodeFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ConfirmYourPostcodeView,
                                        personalDetailsValidationService: PersonalDetailsValidationService,
                                        npsFMNService: NPSFMNService,
                                        auditService: AuditService
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ConfirmYourPostcodePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ConfirmYourPostcodePage, value))
            _ <- sessionRepository.set(updatedAnswers)
            pdvData <- personalDetailsValidationService.getPersonalDetailsValidationByNino(request.nino.getOrElse(""))
            redirectBasedOnMatch <- pdvData match {
              case Some(pdvValidData) => pdvValidData.npsPostCode match {
                case Some(npsPostCode) if npsPostCode.equalsIgnoreCase(value) =>
                  auditService.audit(AuditUtils.buildAuditEvent(pdvData.flatMap(_.personalDetails),
                    "FindYourNinoConfirmPostcode",
                    pdvData.map(_.validationStatus).getOrElse(""),
                    pdvData.map(_.CRN.getOrElse("")).getOrElse(""),
                    None,
                    Some(value),
                    Some("true"),
                    None,
                    None,
                    None
                  ))
                  npsLetterChecks(pdvValidData, npsPostCode, mode, updatedAnswers)
                case None =>
                  auditService.audit(AuditUtils.buildAuditEvent(pdvData.flatMap(_.personalDetails),
                    "FindYourNinoConfirmPostcode",
                    pdvData.map(_.validationStatus).getOrElse(""),
                    pdvData.map(_.CRN.getOrElse("")).getOrElse(""),
                    None,
                    Some(value),
                    Some("false"),
                    None,
                    None,
                    None
                  ))
                  Future(Redirect(routes.TechnicalErrorController.onPageLoad()))
                case _ =>
                  auditService.audit(AuditUtils.buildAuditEvent(pdvData.flatMap(_.personalDetails),
                    "FindYourNinoConfirmPostcode",
                    pdvData.map(_.validationStatus).getOrElse(""),
                    pdvData.map(_.CRN.getOrElse("")).getOrElse(""),
                    None,
                    Some(value),
                    Some("false"),
                    None,
                    None,
                    None
                  ))
                  Future(Redirect(routes.EnteredPostCodeNotFoundController.onPageLoad(mode = NormalMode)))
              }
              case None => Future(Redirect(routes.TechnicalErrorController.onPageLoad()))
            }
          } yield redirectBasedOnMatch
        }
      )
  }

  def npsLetterChecks(personalDetailsResponse: PDVResponseData, npsPostCode: String, mode: Mode, updatedAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Result] = {
    personalDetailsResponse.personalDetails match {
      case Some(personalDetails: PersonalDetails) =>
        for {
          status <- npsFMNService.updateDetails(personalDetails.nino.nino, getNPSFMNRequest(personalDetails, npsPostCode))
        } yield status match {
          case LetterIssuedResponse() =>
            Redirect(routes.NINOLetterPostedConfirmationController.onPageLoad())
          case RLSDLONFAResponse(responseStatus, responseMessage) =>
            auditService.audit(AuditUtils.buildAuditEvent(Some(personalDetails),
              "FindYourNinoError",
              personalDetailsResponse.validationStatus,
              personalDetailsResponse.CRN.getOrElse(""),
              None,
              None,
              None,
              Some("/confirm-your-postcode"),
              Some(responseStatus.toString),
              Some(responseMessage)
            ))
            Redirect(routes.SendLetterErrorController.onPageLoad(mode))
          case TechnicalIssueResponse(responseStatus, responseMessage) =>
            auditService.audit(AuditUtils.buildAuditEvent(Some(personalDetails),
              "FindYourNinoError",
              personalDetailsResponse.validationStatus,
              personalDetailsResponse.CRN.getOrElse(""),
              None,
              None,
              None,
              Some("/confirm-your-postcode"),
              Some(responseStatus.toString),
              Some(responseMessage)
            ))
            Redirect(routes.TechnicalErrorController.onPageLoad())
          case _ =>
            logger.warn("Unknown NPS FMN API response")
            Redirect(routes.TechnicalErrorController.onPageLoad())
        }
      case None => Future(Redirect(routes.TechnicalErrorController.onPageLoad()))
    }
  }

  private def getNPSFMNRequest(personDetails: PersonalDetails, npsPostCode: String): NPSFMNRequest =
      NPSFMNRequest(
        personDetails.firstName,
        personDetails.lastName,
        personDetails.dateOfBirth.toString,
        npsPostCode
      )
}
