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
import forms.HaveSetUpGGUserIDFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.HaveSetUpGGUserIDPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HaveSetUpGGUserIDView

import scala.concurrent.{ExecutionContext, Future}
import java.time.{Clock, Instant}

class HaveSetUpGGUserIDController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       navigator: Navigator,
                                       unauthenticatedIdentifierAction: UnauthenticatedIdentifierActionImpl,
                                       getData: DataRetrievalAction,
                                       formProvider: HaveSetUpGGUserIDFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: HaveSetUpGGUserIDView,
                                       clock: Clock
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode = NormalMode): Action[AnyContent] = (unauthenticatedIdentifierAction andThen getData) {
    implicit request =>

      val preparedForm = request.userAnswers match {
        case Some(value) => value.get(HaveSetUpGGUserIDPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        case None => form
      }
      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (unauthenticatedIdentifierAction andThen getData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(
              request.userAnswers.getOrElse(
                UserAnswers(
                  id = request.userId,
                  lastUpdated = Instant.now(clock)
                )
              ).set(HaveSetUpGGUserIDPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(HaveSetUpGGUserIDPage, mode, updatedAnswers))
      )
  }
}
