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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.{IndividualDetailsRepository, PersonalDetailsValidationRepository, SessionRepository}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.FMNConstants.EmptyString
import views.html.NINOLetterPostedConfirmationView

import javax.inject.Inject

class NINOLetterPostedConfirmationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireValidData: ValidCustomerDataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: NINOLetterPostedConfirmationView,
                                       sessionRepository: SessionRepository,
                                       individualDetailsRepository: IndividualDetailsRepository,
                                       personalDetailsValidationRepository: PersonalDetailsValidationRepository
                                     ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData) {
    implicit request =>
      val nino = request.session.data.getOrElse("nino", EmptyString)

      // Letter posted so invalidate the cache to prevent further attempts to request a letter in the same session
      invalidateCache(nino, request.userId)

      Ok(view())
  }
  private def invalidateCache(nino: String, userId: String) = {
    personalDetailsValidationRepository.clear(nino)
    individualDetailsRepository.clear(nino)
    sessionRepository.clear(userId)
  }

}
