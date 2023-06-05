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
import play.api.{Configuration, Environment}

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.ViewNinoInPTAView

import scala.concurrent.{ExecutionContext, Future}

class ViewNinoInPTAController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       authConnector: AuthConnector,
                                       view: ViewNinoInPTAView,
                                     )(implicit frontendAppConfig: FrontendAppConfig,
                                       config: Configuration,
                                       env: Environment,
                                       ec: ExecutionContext,
                                       cc: MessagesControllerComponents) extends FMNBaseController(authConnector) with I18nSupport {

  implicit val loginContinueUrl: Call = routes.ViewNinoInPTAController.onPageLoad

  def onPageLoad: Action[AnyContent] = authorisedAsFMNUser async {
    implicit request => {
      implicit val m = cc.messagesApi.preferred(request.request)
      Future(Ok(view()(request.request, implicitly)))
    }
  }
}
