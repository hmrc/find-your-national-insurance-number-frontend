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

import config.FrontendAppConfig
import controllers.actions._
import forms.SelectAlternativeServiceFormProvider
import models.{Mode, SelectAlternativeService}
import navigation.Navigator
import org.apache.commons.lang3.StringUtils
import pages.SelectAlternativeServicePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AuditService, PersonalDetailsValidationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SendLetterErrorView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SendLetterErrorController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireValidData: ValidCustomerDataRequiredAction,
  view: SendLetterErrorView,
  formProvider: SelectAlternativeServiceFormProvider,
  personalDetailsValidationService: PersonalDetailsValidationService,
  auditService: AuditService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[SelectAlternativeService] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData() andThen requireValidData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(SelectAlternativeServicePage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData() andThen requireValidData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value => {
            personalDetailsValidationService
              .getPersonalDetailsValidationByNino(request.session.data.getOrElse("nino", StringUtils.EMPTY))
              .map(pdv => auditService.findYourNinoOptionChosen(pdv, value.toString, request.origin))
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectAlternativeServicePage, value))
              _              <- sessionRepository.setUserAnswers(request.userId, updatedAnswers)
            } yield Redirect(navigator.nextPage(SelectAlternativeServicePage, mode, updatedAnswers))
          }
        )
  }
}
