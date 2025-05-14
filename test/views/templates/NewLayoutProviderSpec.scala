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

package views.templates

import config.FrontendAppConfig
import controllers.routes
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.sca.models.BannerConfig
import uk.gov.hmrc.sca.services.WrapperService
import views.html.components.{AdditionalScript, HeadBlock}
import views.html.templates.NewLayoutProvider

class NewLayoutProviderSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "NewLayoutProvider" should {
    "render the standard layout with the correct parameters" in {

      val mockWrapperService   = mock[WrapperService]
      val mockAdditionalScript = mock[AdditionalScript]
      val mockHeadBlock        = mock[HeadBlock]
      val mockAppConfig        = mock[FrontendAppConfig]

      when(mockHeadBlock()).thenReturn(Html("<head></head>"))

      when(mockAppConfig.showAlphaBanner).thenReturn(true)
      when(mockAppConfig.showBetaBanner).thenReturn(false)
      when(mockAppConfig.showHelpImproveBanner).thenReturn(true)

      implicit val mockRequest: Request[_] = FakeRequest()
      implicit val mockMessages: Messages  = mock[Messages]

      val layoutProvider = new NewLayoutProvider(
        mockWrapperService,
        mockAdditionalScript,
        mockHeadBlock,
        mockAppConfig
      )

      val bannerConfig = BannerConfig(
        showAlphaBanner = true,
        showBetaBanner = false,
        showHelpImproveBanner = true
      )

      val contentBlock = Html("<p>Test Content</p>")
      val pageTitle    = "Test Page"

      when(
        mockWrapperService.standardScaLayout(
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          ArgumentMatchers.eq(routes.KeepAliveController.keepAlive.url),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any()
        )(any(), any())
      ).thenReturn(Html("<html></html>"))

      val result = layoutProvider(
        pageTitle = pageTitle,
        showBackLinkJS = true,
        timeout = true,
        showSignOut = false,
        stylesheets = None,
        fullWidth = false,
        accountHome = false,
        yourProfileActive = false,
        hideAccountMenu = false,
        backLinkUrl = None,
        disableSessionExpired = false,
        sidebarContent = None,
        messagesActive = false,
        showSignOutInHeader = true,
        bannerConfig = bannerConfig
      )(contentBlock)

      result shouldBe a[Html]

    }
  }
}
