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

package views.html.templates

import config.FrontendAppConfig
import models.OriginType
import models.OriginType.FMN
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.{Request, RequestHeader}
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.hmrcstandardpage.ServiceURLs
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.sca.models.BannerConfig
import uk.gov.hmrc.sca.services.WrapperService
import views.html.components.{AdditionalScript, HeadBlock}

import javax.inject.Inject

trait LayoutProvider {

  private lazy val defaultBannerConfig: BannerConfig = BannerConfig(
    showAlphaBanner = false,
    showBetaBanner = false,
    showHelpImproveBanner = true
  )
  // noinspection ScalaStyle
  def apply(
    pageTitle: String,
    showBackLinkJS: Boolean = true,
    timeout: Boolean = true,
    showSignOut: Boolean = false,
    stylesheets: Option[Html] = None,
    fullWidth: Boolean = false,
    accountHome: Boolean = false,
    yourProfileActive: Boolean = false,
    hideAccountMenu: Boolean = false,
    backLinkUrl: Option[String] = None,
    disableSessionExpired: Boolean = false,
    sidebarContent: Option[Html] = None,
    messagesActive: Boolean = false,
    showSignOutInHeader: Boolean = true,
    bannerConfig: BannerConfig = defaultBannerConfig
  )(contentBlock: Html)(implicit
    request: RequestHeader,
    messages: Messages
  ): HtmlFormat.Appendable
}

class NewLayoutProvider @Inject() (
  wrapperService: WrapperService,
  additionalScript: AdditionalScript,
  headBlock: HeadBlock,
  appConfig: FrontendAppConfig
) extends LayoutProvider
    with Logging {

  private lazy val newLayoutBannerConfig: BannerConfig = BannerConfig(
    showAlphaBanner = appConfig.showAlphaBanner,
    showBetaBanner = appConfig.showBetaBanner,
    showHelpImproveBanner = appConfig.showHelpImproveBanner
  )

  // noinspection ScalaStyle
  override def apply(
    pageTitle: String,
    showBackLinkJS: Boolean,
    timeout: Boolean,
    showSignOut: Boolean,
    stylesheets: Option[Html],
    fullWidth: Boolean,
    accountHome: Boolean,
    yourProfileActive: Boolean,
    hideAccountMenu: Boolean,
    backLinkUrl: Option[String],
    disableSessionExpired: Boolean,
    sidebarContent: Option[Html],
    messagesActive: Boolean,
    showSignOutInHeader: Boolean,
    bannerConfig: BannerConfig
  )(contentBlock: Html)(implicit
    request: RequestHeader,
    messages: Messages
  ): HtmlFormat.Appendable = {

    val keepAliveUrl = controllers.routes.KeepAliveController.keepAlive.url
    val continueUrl  = Some(RedirectUrl(appConfig.getFeedbackSurveyUrl(OriginType.FMN)))
    val origin       = Some(OriginType.FMN)
    val signOutUrl   = if (showSignOutInHeader) {
      Some(controllers.auth.routes.AuthController.signout(continueUrl, origin).url)
    } else None

    wrapperService.standardScaLayout(
      disableSessionExpired = !timeout,
      content = contentBlock,
      pageTitle = Some(pageTitle),
      serviceURLs = ServiceURLs(
        signOutUrl = signOutUrl
      ),
      showBackLinkJS = showBackLinkJS,
      backLinkUrl = backLinkUrl,
      scripts = Seq(additionalScript()),
      styleSheets = stylesheets.toSeq :+ headBlock(),
      fullWidth = fullWidth,
      hideMenuBar = true,
      bannerConfig = newLayoutBannerConfig,
      keepAliveUrl = keepAliveUrl
    )
  }
}
