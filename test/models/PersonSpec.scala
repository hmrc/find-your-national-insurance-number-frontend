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
import java.time.LocalDate
import uk.gov.hmrc.domain.Nino
import models.Person

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
}
