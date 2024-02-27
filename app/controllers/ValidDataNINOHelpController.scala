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
import controllers.actions.{ValidCustomerDataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.ValidDataNINOHelpFormProvider
import models.{Mode, NormalMode}
import navigation.Navigator
import org.apache.commons.lang3.StringUtils
import pages.ValidDataNINOHelpPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AuditService, PersonalDetailsValidationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.AuditUtils
import views.html.ValidDataNINOHelpView
import play.api.mvc.Codec.utf_8

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ValidDataNINOHelpController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             sessionRepository: SessionRepository,
                                             navigator: Navigator,
                                             identify: IdentifierAction,
                                             getData: DataRetrievalAction,
                                             requireValidData: ValidCustomerDataRequiredAction,
                                             formProvider: ValidDataNINOHelpFormProvider,
                                             view: ValidDataNINOHelpView,
                                             auditService: AuditService,
                                             personalDetailsValidationService: PersonalDetailsValidationService,
                                             val controllerComponents: MessagesControllerComponents
                                  )(implicit ec: ExecutionContext, appConfig: FrontendAppConfig) extends FrontendBaseController with I18nSupport with Logging {

  val form = formProvider()

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
          personalDetailsValidationService.getPersonalDetailsValidationByNino(request.session.data.getOrElse("nino", "")).map(
            pdv =>
              auditService.audit(AuditUtils.buildAuditEvent(pdv.flatMap(_.personalDetails),
                auditType = "FindYourNinoOptionChosen",
                validationOutcome = pdv.map(_.validationStatus).getOrElse("failure"),
                identifierType = pdv.map(_.CRN.getOrElse("")).getOrElse(""),
                findMyNinoOption = Some(value.toString)
              ))
          )
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ValidDataNINOHelpPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ValidDataNINOHelpPage, mode, updatedAnswers))
        }
      )
  }
}
