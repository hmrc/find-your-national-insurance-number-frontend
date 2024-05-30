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
import services.SessionCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.FMNConstants.EmptyString
import views.html.NINOLetterPostedConfirmationView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class NINOLetterPostedConfirmationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: IdentifierAction,
                                       getData: DataRetrievalAction,
                                       requireValidData: ValidCustomerDataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: NINOLetterPostedConfirmationView,
                                       sessionCacheService: SessionCacheService
                                     ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireValidData) {
    implicit request =>
      val nino = request.session.data.getOrElse("nino", EmptyString)
      sessionCacheService.invalidateCache(nino, request.userId)
      Ok(view(LocalDate.now.format(DateTimeFormatter.ofPattern("d MMMM uuuu"))))
  }

}
