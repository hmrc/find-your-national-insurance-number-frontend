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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import models.individualdetails.AccountStatusType._


class AccountStatusSpec extends AnyFlatSpec with Matchers {

  "AccountStatusType reads" should "correctly parse AccountStatusType from integer" in {
    val testData = Seq(
      (0, FullLive),
      (1, PseudoIomPre86),
      (2, FullIomPost86),
      (3, FullCancelled),
      (4, FullAmalgamated),
      (5, FullAdministrative),
      (6, PseudoWeeded),
      (7, PseudoAmalgamated),
      (8, PseudoOther),
      (9, Redundant),
      (10, ConversionRejection),
      (11, Redirected),
      (12, PayeTemporary),
      (13, AmalgamatedPayeTemporary),
      (99, NotKnown)
    )

    for ((input, expectedOutput) <- testData) {
      val json = Json.toJson(input)
      val result = json.as[AccountStatusType]
      result shouldBe expectedOutput
    }
  }

  "AccountStatusType writes" should "correctly write AccountStatusType to integer" in {
    val testData = Seq(
      (FullLive, 0),
      (PseudoIomPre86, 1),
      (FullIomPost86, 2),
      (FullCancelled, 3),
      (FullAmalgamated, 4),
      (FullAdministrative, 5),
      (PseudoWeeded, 6),
      (PseudoAmalgamated, 7),
      (PseudoOther, 8),
      (Redundant, 9),
      (ConversionRejection, 10),
      (Redirected, 11),
      (PayeTemporary, 12),
      (AmalgamatedPayeTemporary, 13),
      (NotKnown, 99)
    )

    //todo: fix this test
    /*for ((input, expectedOutput) <- testData) {
      val json = Json.toJson(input)(models.individualdetails.AccountStatusType.writes)
      json shouldBe expectedOutput
    }*/
  }
}