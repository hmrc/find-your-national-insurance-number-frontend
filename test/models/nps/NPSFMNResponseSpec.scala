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

package models.nps

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class NPSFMNResponseSpec extends AnyFlatSpec with Matchers {

  "NPSFMNResponse" should "serialize and deserialize correctly" in {
    val response = NPSFMNResponse(
      origin = "NPS",
      response = Response(
        JsonServiceError(
          requestURL = "/some/url",
          message = "some message",
          appStatusMessageCount = 2,
          appStatusMessageList = AppStatusMessageList(List("msg1", "msg2"))
        )
      )
    )

    val json   = Json.toJson(response)
    val parsed = json.as[NPSFMNResponse]

    parsed shouldBe response
  }

  "NPSFMNResponseWithFailures" should "serialize and deserialize correctly" in {
    val response = NPSFMNResponseWithFailures(
      origin = "NPS",
      response = ResponseWithFailures(List(Failure("ERR", "Something went wrong")))
    )

    val json   = Json.toJson(response)
    val parsed = json.as[NPSFMNResponseWithFailures]

    parsed shouldBe response
  }
}
