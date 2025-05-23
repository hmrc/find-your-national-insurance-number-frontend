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

package controllers

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Lang
import play.api.mvc.Results
import config.FrontendAppConfig
import org.mockito.Mockito.when
import uk.gov.hmrc.play.language.LanguageUtils

class LanguageSwitchControllerSpec extends SpecBase with Results with MockitoSugar {

  val appConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val languageUtils: LanguageUtils = mock[LanguageUtils]

  val controller = new LanguageSwitchController(appConfig, languageUtils, cc)
  "LanguageSwitchController" - {

    "return the fallback URL" in {
      when(appConfig.languageMap).thenReturn(Map("en" -> Lang("en"), "fr" -> Lang("fr")))
      when(appConfig.fmnGuidancePageUrl).thenReturn("/foo")
      val result = controller.fallbackURL
      result mustBe "/foo"
    }

    "return the language map" in {
      val expectedLanguageMap = Map("en" -> Lang("en"), "fr" -> Lang("fr"))
      when(appConfig.languageMap).thenReturn(expectedLanguageMap)
      val result              = controller.languageMap
      result mustBe expectedLanguageMap
    }
  }
}
