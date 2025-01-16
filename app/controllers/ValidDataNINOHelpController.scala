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
import config.FrontendAppConfig
import controllers.actions.{DataRetrievalAction, IdentifierAction, ValidDataRequiredAction}
import forms.ValidDataNINOHelpFormProvider
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.ValidDataNINOHelpPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Codec.utf_8
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AuditService, PersonalDetailsValidationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ValidDataNINOHelpView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidDataNINOHelpController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             sessionRepository: SessionRepository,
                                             navigator: Navigator,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalAction,
                                             requireValidData: ValidDataRequiredAction,
                                             formProvider: ValidDataNINOHelpFormProvider,
                                             view: ValidDataNINOHelpView,
                                             auditService: AuditService,
                                             personalDetailsValidationService: PersonalDetailsValidationService,
                                             val controllerComponents: MessagesControllerComponents,
                                             pdvResponseHandler: PDVNinoExtractor
                                  )(implicit ec: ExecutionContext, appConfig: FrontendAppConfig) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode = NormalMode): Action[AnyContent] = (identify andThen getData andThen requireValidData) {
    implicit request =>
        val preparedForm = request.userAnswers.get(ValidDataNINOHelpPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireValidData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value => {
          personalDetailsValidationService.getPersonalDetailsValidationByNino(pdvResponseHandler.getNino(request.pdvResponse.get).getOrElse("")).map(
            pdv => auditService.findYourNinoOptionChosen(pdv, value.toString, request.userAnswers.get(OriginCacheable))
          )
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ValidDataNINOHelpPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ValidDataNINOHelpPage, mode, updatedAnswers))
        }
      )
  }
}
