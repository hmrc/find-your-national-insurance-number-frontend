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

package models

import models.SelectAlternativeService.PhoneHmrc
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import pages.{ConfirmIdentityPage, SelectAlternativeServicePage, SelectNINOLetterAddressPage, ValidDataNINOHelpPage}
import play.api.libs.json.{JsValue, Json}

import java.time.Instant

class SessionDataSpec extends AnyWordSpec with GuiceOneAppPerSuite with Matchers with BeforeAndAfterEach {

  "reads" must {
    "read correctly in new format" in {
      val userAnswers          = UserAnswers().setOrException(ConfirmIdentityPage, true)
      val sessionData          = SessionData(userAnswers = userAnswers, origin = Some(OriginType.PDV), id = "id")
      val sessionJson: JsValue = Json.toJson(sessionData)
      val readInSessionData    = sessionJson.as[SessionData]
      readInSessionData.origin mustBe sessionData.origin
      readInSessionData.userAnswers mustBe sessionData.userAnswers
      readInSessionData.id mustBe sessionData.id
      readInSessionData.isOldFormat mustBe false
    }

    "read correctly in old format" in {
      val sessionJson: JsValue = Json.parse(
        """{
          |"_id":"session-803bec74-435d-42a4-8b3f-204baca7e009",
          |"data":{"origin":"PDV","validDataNINOHelp":true,"selectNINOLetterAddress":false,"selectAlternativeService":"phoneHMRC"},
          |"lastUpdated":{"$date":{"$numberLong":"1738582855820"}}}""".stripMargin
      )

      val readInSessionData = sessionJson.as[SessionData]
      readInSessionData.origin mustBe Some(OriginType.PDV)
      readInSessionData.userAnswers.get(ValidDataNINOHelpPage) mustBe Some(true)
      readInSessionData.userAnswers.get(SelectNINOLetterAddressPage) mustBe Some(false)
      readInSessionData.userAnswers.get(SelectAlternativeServicePage) mustBe Some(PhoneHmrc)
      readInSessionData.id mustBe "session-803bec74-435d-42a4-8b3f-204baca7e009"
      readInSessionData.lastUpdated mustBe Instant.ofEpochMilli(1738582855820L)
      readInSessionData.isOldFormat mustBe true

    }
  }

}
