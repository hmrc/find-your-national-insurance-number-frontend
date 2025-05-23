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

package models.upstreamfailure

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import play.api.libs.json.Json

class FailureSpec extends AnyFlatSpec with Matchers {

  "Failure" should "serialize to JSON" in {
    val failure = Failure("ERR1", "Invalid input")
    val json    = Json.toJson(failure)
    json shouldBe Json.obj("code" -> "ERR1", "reason" -> "Invalid input")
  }

  it should "deserialize from JSON" in {
    val json   = Json.parse("""{ "code": "ERR1", "reason": "Invalid input" }""")
    val result = json.as[Failure]
    result shouldBe Failure("ERR1", "Invalid input")
  }
}
