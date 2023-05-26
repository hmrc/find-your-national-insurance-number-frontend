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
import forms.SelectNINOLetterAddressFormProvider
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.SelectNINOLetterAddressPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SelectNINOLetterAddressView

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelectNINOLetterAddressController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: SessionRepository,
                                       navigator: Navigator,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: SelectNINOLetterAddressFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: SelectNINOLetterAddressView,
                                       clock: Clock
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) {
    implicit request =>

      //will require implementation of userData
      val postCode: String = "FX97 4TU"

      //will need to be changed to retrieve user's NINO
      val preparedForm = request.userAnswers match {
        case Some(value) => value.get(SelectNINOLetterAddressPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        case None => form
      }

      Ok(view(preparedForm, mode, postCode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode, postcode = ""))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.getOrElse(
              UserAnswers(
                id = request.userId,
                lastUpdated = Instant.now(clock)
              )
            ).set(SelectNINOLetterAddressPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(SelectNINOLetterAddressPage, mode, updatedAnswers))
      )
  }
}
