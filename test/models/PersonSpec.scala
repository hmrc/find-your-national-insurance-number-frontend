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
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

class PersonSpec extends AnyFlatSpec with Matchers {

  "PersonOps" should "correctly get first name" in {
    val person = Person(Some("John"), None, None, None, None, None, None, None, None)
    person.getFirstName shouldBe "John"
  }

  it should "correctly get last name" in {
    val person = Person(None, None, Some("Doe"), None, None, None, None, None, None)
    person.getLastName shouldBe "Doe"
  }

  it should "correctly get date of birth" in {
    val person = Person(None, None, None, None, None, None, None, Some(LocalDate.of(1990, 1, 1)), None)
    person.getDateOfBirth shouldBe "1990-01-01"
  }

  it should "return empty string if first name is not present" in {
    val person = Person(None, None, None, None, None, None, None, None, None)
    person.getFirstName shouldBe ""
  }

  it should "return empty string if last name is not present" in {
    val person = Person(None, None, None, None, None, None, None, None, None)
    person.getLastName shouldBe ""
  }

  it should "return empty string if date of birth is not present" in {
    val person = Person(None, None, None, None, None, None, None, None, None)
    person.getDateOfBirth shouldBe ""
  }

  "Person" should "serialize and deserialize correctly" in {
    val person = Person(
      firstName = Some("John"),
      middleName = Some("H."),
      lastName = Some("Doe"),
      initials = Some("J.H.D."),
      title = Some("Mr"),
      honours = Some("OBE"),
      sex = Some("M"),
      dateOfBirth = Some(LocalDate.of(1985, 5, 15)),
      nino = Some(Nino("AA000003B"))
    )

    val json   = Json.toJson(person)
    val parsed = json.as[Person]

    parsed shouldBe person
  }

  "PersonDetails" should "serialize and deserialize correctly" in {
    val address = Address(
      Some("Line 1"),
      Some("Line 2"),
      Some("Line 3"),
      Some("Line 4"),
      Some("Line 5"),
      Some("AA1 1AA"),
      Some("Country"),
      Some(LocalDate.now()),
      Some(LocalDate.now().plusDays(1)),
      Some("Type"),
      isRls = false
    )

    val person = Person(
      firstName = Some("Alice"),
      middleName = None,
      lastName = Some("Smith"),
      initials = None,
      title = Some("Ms"),
      honours = None,
      sex = Some("F"),
      dateOfBirth = Some(LocalDate.of(1990, 1, 1)),
      nino = Some(Nino("AA000004C"))
    )

    val personDetails = PersonDetails(
      person = person,
      address = Some(address),
      correspondenceAddress = None
    )

    val json   = Json.toJson(personDetails)
    val parsed = json.as[PersonDetails]

    parsed                    shouldBe personDetails
    personDetails.getPostCode shouldBe "AA1 1AA"
  }

}
