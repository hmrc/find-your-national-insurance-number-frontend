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
import forms.ValidDataNINOMatchedNINOHelpFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.ValidDataNINOMatchedNINOHelpPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AuditService, PersonalDetailsValidationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.AuditUtils
import views.html.ValidDataNINOMatchedNINOHelpView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ValidDataNINOMatchedNINOHelpController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         sessionRepository: SessionRepository,
                                         navigator: Navigator,
                                         identify: IdentifierAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: ValidDataNINOMatchedNINOHelpFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view: ValidDataNINOMatchedNINOHelpView,
                                         auditService: AuditService,
                                         personalDetailsValidationService: PersonalDetailsValidationService
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode = NormalMode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(ValidDataNINOMatchedNINOHelpPage) match {
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
          personalDetailsValidationService.getPersonalDetailsValidationByNino(request.nino.getOrElse("")).onComplete {
            case Success(pdv) =>
              auditService.audit(AuditUtils.buildAuditEvent(pdv.flatMap(_.personalDetails),
                "FindYourNinoOptionChosen",
                pdv.map(_.validationStatus).getOrElse(""),
                pdv.map(_.CRN.getOrElse("")).getOrElse(""),
                Some(value.toString),
                None,
                None,
                None,
                None,
                None
              ))
            case Failure(ex) =>
              logger.warn(ex.getMessage)
          }
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ValidDataNINOMatchedNINOHelpPage, value))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ValidDataNINOMatchedNINOHelpPage, mode, updatedAnswers))
        }
      )
  }
}
