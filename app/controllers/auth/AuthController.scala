/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.auth

import config.FrontendAppConfig
import controllers.actions.IdentifierAction
import models.OriginType
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrlPolicy.Id
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, RedirectUrl, RedirectUrlPolicy}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.sca.services.WrapperService

import javax.inject.Inject

class AuthController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  config: FrontendAppConfig,
  identify: IdentifierAction,
  wrapperService: WrapperService
) extends FrontendBaseController
    with I18nSupport {

  def signout(continueUrl: Option[RedirectUrl], origin: Option[OriginType]): Action[AnyContent] =
    Action {
      val mdtpTrustedDomains: Set[String] = config.trustedDomains
      val policy: RedirectUrlPolicy[Id]   = AbsoluteWithHostnameFromAllowlist(mdtpTrustedDomains)
      val safeUrl: Option[String]         = wrapperService.safeSignoutUrl(continueUrl)

      safeUrl
        .orElse(origin.map(config.getFeedbackSurveyUrl))
        .fold(BadRequest("Missing origin")) { (url: String) =>
          Redirect(config.getBasGatewayFrontendSignOutUrl(RedirectUrl(url).get(policy).url))
        }
    }

  def timeOut: Action[AnyContent] = Action {
    Redirect(controllers.auth.routes.SignedOutController.onPageLoad).withNewSession
  }

  def redirectToSMN(): Action[AnyContent] = identify {
    Redirect(config.storeMyNinoUrl)
  }

  def redirectToRegister(continueUrl: Option[RedirectUrl]): Action[AnyContent] = {
    val mdtpTrustedDomains: Set[String] = config.trustedDomains
    val policy: RedirectUrlPolicy[Id]   = AbsoluteWithHostnameFromAllowlist(mdtpTrustedDomains)
    val url: String                     = continueUrl.getOrElse(RedirectUrl(config.registerUrl)).get(policy).url

    val params: Map[String, Seq[Serializable]] = Map(
      "origin"      -> Seq(config.appName),
      "continueUrl" -> Seq(url),
      "accountType" -> Seq("Individual")
    )
    val stringParams: Map[String, Seq[String]] = params.view.mapValues(_.map(_.toString)).toMap

    Action {
      Redirect(config.registerUrl, stringParams)
    }
  }

}
