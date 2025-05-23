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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class CountrySpec extends AnyFlatSpec with Matchers {
  "Country" should "serialize and deserialize correctly" in {
    val country = Country("United Kingdom")
    val json    = Json.toJson(country)
    json             shouldBe Json.parse("""{ "countryName": "United Kingdom" }""")
    json.as[Country] shouldBe country
  }
}
