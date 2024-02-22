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

import controllers.actions._
import forms.TechnicalErrorServiceFormProvider
import models.Mode
import navigation.Navigator
import org.apache.commons.lang3.StringUtils
import pages.TechnicalErrorPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.{SessionRepository, TryAgainCountRepository}
import services.{AuditService, PersonalDetailsValidationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.AuditUtils
import views.html.TechnicalErrorView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TechnicalErrorController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       tryAgainCountRepository: TryAgainCountRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: TechnicalErrorServiceFormProvider,
                                       personalDetailsValidationService: PersonalDetailsValidationService,
                                       auditService: AuditService,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: TechnicalErrorView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) async {
    implicit request =>

      val preparedForm = request.userAnswers.get(TechnicalErrorPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      for {
        retryAllowed <- tryAgainCountRepository.findById(request.userId).map {
          case Some (value) => if (value.count >= 5) {false} else {true}
          case None => true
        }
      } yield Ok(view(preparedForm, mode, retryAllowed))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          for {
            retryAllowed <- tryAgainCountRepository.findById(request.userId).map {
              case Some(value) => if (value.count >= 5) {
                false
              } else {
                true
              }
              case None => true
            }
          } yield BadRequest(view(formWithErrors, mode, retryAllowed)),

        value => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(TechnicalErrorPage, value))
            _ <- sessionRepository.set(updatedAnswers)
            pdvData <- personalDetailsValidationService.getPersonalDetailsValidationByNino(request.session.data.getOrElse("nino", StringUtils.EMPTY))
          } yield {
            val personalDetails = pdvData.flatMap(_.personalDetails)
            val postcode: String = personalDetails.flatMap(_.postCode).getOrElse(StringUtils.EMPTY)
            auditService.audit(AuditUtils.buildAuditEvent(personalDetails,
              auditType = "FindYourNinoOptionChosen",
              validationOutcome = pdvData.map(_.validationStatus).getOrElse("failure"),
              identifierType = pdvData.map(_.CRN.getOrElse("")).getOrElse(""),
              findMyNinoOption = Some(value.toString)
            ))
            if (value.toString == "tryAgain") {
              tryAgainCountRepository.insertOrIncrement(request.userId)
              if (postcode.nonEmpty) {
                Redirect(routes.SelectNINOLetterAddressController.onPageLoad())
              } else {
                Redirect(routes.ConfirmYourPostcodeController.onPageLoad())
              }
            } else {
              Redirect(navigator.nextPage(TechnicalErrorPage, mode, updatedAnswers))
            }
          }
        }
      )
  }
}