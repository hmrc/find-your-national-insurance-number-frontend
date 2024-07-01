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

package models.pdv

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.domain.Nino

import java.time.{Instant, LocalDate}

class PDVResponseDataSpec extends AnyFlatSpec with Matchers {

  "An PDV ResponseData and PersonalDetails" should "have valid data" in {
    val personalDetails: PersonalDetails = PersonalDetails(
      "firstName",
      "lastName",
      Nino("AA123456B"),
      Some("NE12 1ZZ"),
      LocalDate.of(2024, 1, 1)
    )

    val pdvResponseData: PDVResponseData = PDVResponseData(
      "id",
      ValidationStatus.Success,
      Some(personalDetails),
      Instant.now,
      Some("reason"),
      Some(true),
      Some("CRN"),
      None
    )
    import uk.gov.hmrc.domain.Nino.isValid
    pdvResponseData.validationStatus should be (ValidationStatus.Success)
    pdvResponseData.validCustomer should be (Some(true))
    isValid(personalDetails.nino.nino) should be (true)
    pdvResponseData.getFirstName should be ("firstName")
    pdvResponseData.getLastName should be ("lastName")
    pdvResponseData.getPostCode should be ("NE12 1ZZ")
    pdvResponseData.getNino should be ("AA123456B")
    pdvResponseData.getDateOfBirth should be ("2024-01-01")
    pdvResponseData.CRN should be (Some("CRN"))
    pdvResponseData.npsPostCode should be (None)
  }

  "An PDV ResponseData and PersonalDetails" should "have empty data" in {
    val emptyPersonalDetails: PersonalDetails = PersonalDetails(
      "",
      "",
      Nino("AA123456B"),
      None,
      LocalDate.of(2024, 1, 1)
    )
    val pdvResponseData2: PDVResponseData = PDVResponseData(
      "id",
      ValidationStatus.Success,
      Some(emptyPersonalDetails),
      Instant.now,
      Some("reason"),
      Some(true),
      Some("CRN"),
      None
    )
    pdvResponseData2.getFirstName should be ("")
    pdvResponseData2.getLastName should be ("")
    pdvResponseData2.getPostCode should be ("")

  }

}
