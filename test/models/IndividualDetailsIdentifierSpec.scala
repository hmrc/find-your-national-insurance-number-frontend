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

package models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class IndividualDetailsIdentifierSpec extends AnyFlatSpec with Matchers {

  "IndividualDetailsIdentifier reads" should "correctly parse IndividualDetailsNino" in {
    val json   = Json.parse("\"AB123456B\"")
    val result = json.as[IndividualDetailsNino]
    result shouldBe IndividualDetailsNino("AB123456B")
  }

  it should "correctly parse ChildReferenceNumber" in {
    val json   = Json.parse("\"AA123456\"")
    val result = json.as[ChildReferenceNumber]
    result shouldBe ChildReferenceNumber("AA123456")
  }

  it should "correctly parse TemporaryReferenceNumber" in {
    val json   = Json.parse("\"12345678\"")
    val result = json.as[TemporaryReferenceNumber]
    result shouldBe TemporaryReferenceNumber("12345678")
  }

  it should "throw RuntimeException for invalid identifier" in {
    val json = Json.parse("\"invalid\"")
    assertThrows[RuntimeException] {
      json.as[IndividualDetailsIdentifier]
    }
  }

  it should "correctly write IndividualDetailsNino" in {
    val nino = IndividualDetailsNino("AA123456A")
    val json = Json.toJson(nino)
    json.toString() shouldBe """"AA123456A""""
  }

  it should "correctly write ChildReferenceNumber" in {
    val crn  = ChildReferenceNumber("AA123456")
    val json = Json.toJson(crn)
    json.toString() shouldBe """"AA123456""""
  }

  it should "correctly write TemporaryReferenceNumber" in {
    val trn  = TemporaryReferenceNumber("12345678")
    val json = Json.toJson(trn)
    json.toString() shouldBe """"12345678""""
  }

  "IndividualDetailsNino" should "correctly remove suffix from NINO" in {
    val nino   = IndividualDetailsNino("AB123456C")
    val result = nino.withoutSuffix
    result shouldBe "AB123456"
  }

  it should "return the same NINO if it has no suffix" in {
    val nino   = IndividualDetailsNino("AB123456")
    val result = nino.withoutSuffix
    result shouldBe "AB123456"
  }

}
