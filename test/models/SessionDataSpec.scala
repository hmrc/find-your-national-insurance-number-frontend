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

import models.InvalidDataNINOHelp.PhoneHmrc
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import pages.{ConfirmIdentityPage, InvalidDataNINOHelpPage}
import play.api.libs.json.{JsValue, Json}

class SessionDataSpec extends AnyWordSpec with GuiceOneAppPerSuite with Matchers with BeforeAndAfterEach {

  "reads" must {
    "read correctly in new format" in {
      val userAnswers          = UserAnswers().setOrException(ConfirmIdentityPage, true)
      val sessionData          = SessionData(userAnswers = userAnswers, origin = OriginType.PDV, id = "id")
      val sessionJson: JsValue = Json.toJson(sessionData)
      val readInSessionData    = sessionJson.as[SessionData]
      readInSessionData.origin mustBe sessionData.origin
      readInSessionData.userAnswers mustBe sessionData.userAnswers
      readInSessionData.id mustBe sessionData.id
    }

    "read correctly in old format" in {
      val sessionJson: JsValue = Json.parse("""{
        |    "_id" : "session-43ee12c0-7aa8-4027-8cfb-de8a0f1a1c75",
        |    "data" : {
        |        "origin" : "PDV",
        |        "invalidDataNinoHelp" : "phoneHMRC"
        |    },
        |    "lastUpdated" : ISODate("2025-02-03T11:18:01.960Z")
        |}""".stripMargin)


      val readInSessionData = sessionJson.as[SessionData]
      readInSessionData.origin mustBe OriginType.PDV
      readInSessionData.userAnswers.get(InvalidDataNINOHelpPage) mustBe Some(PhoneHmrc)
      readInSessionData.id mustBe "session-43ee12c0-7aa8-4027-8cfb-de8a0f1a1c75"
    }
  }

}
