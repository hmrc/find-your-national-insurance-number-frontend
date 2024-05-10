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
import controllers.actions.{CL50DataRequiredAction, DataRetrievalAction, IdentifierAction, IdentifierActionIndividual}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.FMNConstants.FMNOrigin
import views.html.TracingWhatYouNeedView

import java.net.URLEncoder
import javax.inject.Inject

class TracingWhatYouNeedController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              identify: IdentifierActionIndividual,
                                              getData: DataRetrievalAction,
                                              requireData: CL50DataRequiredAction,
                                              val controllerComponents: MessagesControllerComponents,
                                              config: FrontendAppConfig,
                                              view: TracingWhatYouNeedView
                                            ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData)  {
    implicit request =>
      val origin        = FMNOrigin
      val redirectUrl   = config.fmnCheckDetailsUrl

      val pdvUrl = s"${config.personalDetailsValidationFrontEnd}/personal-details-validation/start?" +
        s"completionUrl=${URLEncoder.encode(redirectUrl, "UTF-8")}&failureUrl=${URLEncoder.encode(redirectUrl, "UTF-8")}&origin=$origin"

      Ok(view(pdvUrl))
  }
}
