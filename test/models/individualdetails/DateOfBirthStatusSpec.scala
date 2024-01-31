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

import models.individualdetails.DateOfBirthStatus.{CoegConfirmed, NotKnown, Unverified, Verified}
import models.json.WritesNumber
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{Json, Writes}


class DateOfBirthStatusSpec extends AnyFlatSpec with Matchers {

  implicit val writesUnverified: Writes[DateOfBirthStatus.Unverified.type] = WritesNumber[DateOfBirthStatus.Unverified.type](_ => 0)
  implicit val writesVerified: Writes[DateOfBirthStatus.Verified.type] = WritesNumber[DateOfBirthStatus.Verified.type](_ => 1)
  implicit val writesNotKnown: Writes[DateOfBirthStatus.NotKnown.type] = WritesNumber[DateOfBirthStatus.NotKnown.type](_ => 2)
  implicit val writesCoegConfirmed: Writes[DateOfBirthStatus.CoegConfirmed.type] = WritesNumber[DateOfBirthStatus.CoegConfirmed.type](_ => 3)

  "DateOfBirthStatus" should "be read correctly from JSON" in {
    val json = Json.parse("""0""")
    json.as[DateOfBirthStatus] shouldBe Unverified

    val json1 = Json.parse("""1""")
    json1.as[DateOfBirthStatus] shouldBe Verified

    val json2 = Json.parse("""2""")
    json2.as[DateOfBirthStatus] shouldBe NotKnown

    val json3 = Json.parse("""3""")
    json3.as[DateOfBirthStatus] shouldBe CoegConfirmed
  }

  for{
(input, expectedOutput) <- Seq(
      (Unverified, 0),
      (Verified, 1),
      (NotKnown, 2),
      (CoegConfirmed, 3)
    )
  } {
    s"DateOfBirthStatus $input" should s"be written correctly to JSON" in {
      val json = Json.toJson(input)
      json shouldBe Json.toJson(expectedOutput)
    }

  }

}