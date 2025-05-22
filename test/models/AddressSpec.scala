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
import java.time.LocalDate

class AddressSpec extends AnyFlatSpec with Matchers {

  def fakeAddress: models.Address =
    Address(
      Some("Line 1"),
      Some("Line 2"),
      Some("Line 3"),
      Some("Line 4"),
      Some("Line 5"),
      Some("Postcode"),
      Some("Country"),
      Some(LocalDate.now()),
      Some(LocalDate.now().plusDays(1)),
      Some("Type"),
      isRls = false
    )

  "An Address" should "have a line1" in {
    fakeAddress.line1 should be(Some("Line 1"))
  }

  it should "have a line2" in {
    fakeAddress.line2 should be(Some("Line 2"))
  }

  it should "have a line3" in {
    fakeAddress.line3 should be(Some("Line 3"))
  }

  it should "have a line4" in {
    fakeAddress.line4 should be(Some("Line 4"))
  }

  it should "have a line5" in {
    fakeAddress.line5 should be(Some("Line 5"))
  }

  it should "have a postcode" in {
    fakeAddress.postcode should be(Some("Postcode"))
  }

  it should "have a country" in {
    fakeAddress.country should be(Some("Country"))
  }

  it should "have a startDate" in {
    fakeAddress.startDate should be(Some(LocalDate.now()))
  }

  it should "have an endDate" in {
    fakeAddress.endDate should be(Some(LocalDate.now().plusDays(1)))
  }

  it should "have a type" in {
    fakeAddress.`type` should be(Some("Type"))
  }

  it should "have an isRls" in {
    fakeAddress.isRls should be(false)
  }

  "An Address" should "return None for internationalAddressCountry if the country is in the excluded list" in {
    val addressWithExcludedCountry = fakeAddress.copy(country = Some("GREAT BRITAIN"))
    addressWithExcludedCountry.internationalAddressCountry(addressWithExcludedCountry.country) should be(None)
  }

  it should "return the country for internationalAddressCountry if the country is not in the excluded list" in {
    val addressWithNonExcludedCountry = fakeAddress.copy(country = Some("FRANCE"))
    addressWithNonExcludedCountry.internationalAddressCountry(addressWithNonExcludedCountry.country) should be(
      Some("FRANCE")
    )
  }

  "An Address" should "return true for isWelshLanguageUnit if the postcode is in the Welsh Language Unit postcodes list" in {
    val addressWithWelshPostcode = fakeAddress.copy(postcode = Some("CF145SH"))
    addressWithWelshPostcode.isWelshLanguageUnit should be(true)
  }

  it should "return false for isWelshLanguageUnit if the postcode is not in the Welsh Language Unit postcodes list" in {
    val addressWithNonWelshPostcode = fakeAddress.copy(postcode = Some("AB123CD"))
    addressWithNonWelshPostcode.isWelshLanguageUnit should be(false)
  }

  "Address" should "be able to write to JSON" in {
    val json    = Json.toJson(fakeAddress)(Address.writes)
    val address = json.as[Address](Address.reads)
    address should be(fakeAddress)
  }

  it should "be able to read from JSON" in {
    val json    = Json.toJson(fakeAddress)(Address.writes)
    val address = json.as[Address](Address.reads)
    address should be(fakeAddress)
  }

  "isRls" should "return false if status is 0" in {
    val json    = Json.parse("""{"status": 0}""")
    val address = json.as[Address]
    address.isRls should be(false)
  }

  it should "return true if status is 1" in {
    val json    = Json.parse("""{"status": 1}""")
    val address = json.as[Address]
    address.isRls should be(true)
  }

  it should "return false and log an error if status is not 0 or 1" in {
    val json    = Json.parse("""{"status": 2}""")
    val address = json.as[Address]
    address.isRls should be(false)
  }

  "AddressOps" should "return the postcode if it exists" in {
    val address = Address(
      Some("Line 1"),
      Some("Line 2"),
      Some("Line 3"),
      Some("Line 4"),
      Some("Line 5"),
      Some("Postcode"),
      Some("Country"),
      Some(LocalDate.now()),
      Some(LocalDate.now().plusDays(1)),
      Some("Type"),
      isRls = false
    )
    address.getPostCode should be("Postcode")
  }

  it should "return an empty string if the postcode does not exist" in {
    val address = Address(
      Some("Line 1"),
      Some("Line 2"),
      Some("Line 3"),
      Some("Line 4"),
      Some("Line 5"),
      None,
      Some("Country"),
      Some(LocalDate.now()),
      Some(LocalDate.now().plusDays(1)),
      Some("Type"),
      isRls = false
    )
    address.getPostCode should be("")
  }
}
