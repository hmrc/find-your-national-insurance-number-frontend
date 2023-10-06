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

import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.ValidDataNINOHelpFormProvider
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.ValidDataNINOHelpPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.PersonalDetailsValidationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ValidDataNINOHelpView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidDataNINOHelpController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             sessionRepository: SessionRepository,
                                             navigator: Navigator,
                                             personalDetailsValidationService: PersonalDetailsValidationService,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: ValidDataNINOHelpFormProvider,
                                             view: ValidDataNINOHelpView,
                                             val controllerComponents: MessagesControllerComponents
                                  )(implicit ec: ExecutionContext, appConfig: FrontendAppConfig) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode = NormalMode): Action[AnyContent] = (identify andThen getData andThen requireData) async {
    implicit request =>
      val resp = personalDetailsValidationService.getPDVDataValidationStatus(request.nino.getOrElse("")).map(
        validationStatus =>
          if (validationStatus == "true") {
            val preparedForm = request.userAnswers.get(ValidDataNINOHelpPage) match {
              case None => form
              case Some(value) => form.fill(value)
            }

            Ok(view(preparedForm, mode))
          } else {
            Redirect(controllers.routes.UnauthorisedController.onPageLoad)
          }
      )
      request.nino match {
        case Some(nino) => resp
        case None => Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ValidDataNINOHelpPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ValidDataNINOHelpPage, mode, updatedAnswers))
      )
  }


}
