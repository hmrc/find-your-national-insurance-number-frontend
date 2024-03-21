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

import Helpers.TaxYearResolver
import controllers.actions._
import forms.UpliftOrLetterFormProvider
import models.{Mode, UpliftOrLetter}
import navigation.Navigator
import pages.UpliftOrLetterPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.UpliftOrLetterView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpliftOrLetterController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: UpliftOrLetterFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        taxYearResolver: TaxYearResolver,
                                        view: UpliftOrLetterView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Set[UpliftOrLetter]] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(UpliftOrLetterPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      val cy = taxYearResolver.currentTaxYear.toString
      val ny = (taxYearResolver.currentTaxYear + 1).toString

      Ok(view(preparedForm, cy, ny, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val cy = taxYearResolver.currentTaxYear.toString
      val ny = (taxYearResolver.currentTaxYear + 1).toString

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, cy, ny, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UpliftOrLetterPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(UpliftOrLetterPage, mode, updatedAnswers))
      )
  }
}
