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
import controllers.actions._
import forms.LetterTechnicalErrorFormProvider
import models.{LetterTechnicalError, Mode}
import navigation.Navigator
import org.apache.commons.lang3.StringUtils
import pages.LetterTechnicalErrorPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.{SessionRepository, TryAgainCountRepository}
import services.{AuditService, PersonalDetailsValidationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.LetterTechnicalErrorView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LetterTechnicalErrorController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                sessionRepository: SessionRepository,
                                                tryAgainCountRepository: TryAgainCountRepository,
                                                navigator: Navigator,
                                                identify: IdentifierAction,
                                                getData: DataRetrievalAction,
                                                requireValidData: ValidCustomerDataRequiredAction,
                                                formProvider: LetterTechnicalErrorFormProvider,
                                                personalDetailsValidationService: PersonalDetailsValidationService,
                                                auditService: AuditService,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: LetterTechnicalErrorView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[LetterTechnicalError] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireValidData) async {
    implicit request =>

      // form to not be pre-populated
      val preparedForm = request.userAnswers.get(LetterTechnicalErrorPage) match {
        case _ => form
      }

      for {
        retryAllowed <- tryAgainCountRepository.findById(request.userId).map {
          case Some (value) => if (value.count >= 5) {false} else {true}
          case None => true
        }
      } yield Ok(view(preparedForm, mode, retryAllowed))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireValidData).async {
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
            updatedAnswers <- Future.fromTry(request.userAnswers.set(LetterTechnicalErrorPage, value))
            _ <- sessionRepository.set(updatedAnswers)
            pdvData <- personalDetailsValidationService.getPersonalDetailsValidationByNino(request.session.data.getOrElse("nino", StringUtils.EMPTY))
          } yield {
            val personalDetails = pdvData.flatMap(_.personalDetails)
            val postcode: String = personalDetails.flatMap(_.postCode).getOrElse(StringUtils.EMPTY)
            auditService.findYourNinoOptionChosen(pdvData, value.toString, request.userAnswers.get(OriginCacheable))
            if (value.toString == "tryAgain") {
              tryAgainCountRepository.insertOrIncrement(request.userId)
              if (postcode.nonEmpty) {
                Redirect(routes.SelectNINOLetterAddressController.onPageLoad())
              } else {
                Redirect(routes.ConfirmYourPostcodeController.onPageLoad())
              }
            } else {
              Redirect(navigator.nextPage(LetterTechnicalErrorPage, mode, updatedAnswers))
            }
          }
        }
      )
  }
}