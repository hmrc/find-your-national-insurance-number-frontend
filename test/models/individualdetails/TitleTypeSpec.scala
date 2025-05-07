/*
 * Copyright 2024 HM Revenue & Customs
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

package models.individualdetails

import models.individualdetails.TitleType._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class TitleTypeSpec extends AnyFlatSpec with Matchers {

  "TitleType" should "be read correctly from JSON" in {
    val json = Json.parse("""0""")
    json.as[TitleType] shouldBe NotKnown

    val json1 = Json.parse("""1""")
    json1.as[TitleType] shouldBe Mr

    val json2 = Json.parse("""2""")
    json2.as[TitleType] shouldBe Mrs

    val json3 = Json.parse("""3""")
    json3.as[TitleType] shouldBe Miss

    val json4 = Json.parse("""4""")
    json4.as[TitleType] shouldBe Ms

    val json5 = Json.parse("""5""")
    json5.as[TitleType] shouldBe Dr

    val json6 = Json.parse("""6""")
    json6.as[TitleType] shouldBe Rev
  }

  for {
    (titleType, json) <- Seq(
                           (NotKnown, """0"""),
                           (Mr, """1"""),
                           (Mrs, """2"""),
                           (Miss, """3"""),
                           (Ms, """4"""),
                           (Dr, """5"""),
                           (Rev, """6""")
                         )
  }
    it should s"write $titleType to JSON" in {
      Json.toJson(titleType) shouldBe Json.parse(json)
    }
}
