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

package util

import models.individualdetails.{IndividualDetailsData, IndividualDetailsDataCache}
import models.nps.NPSFMNRequest
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FMNHelperSpec extends AnyFlatSpec with Matchers {

  "splitPostCode" should "remove spaces and insert a space before the last three characters" in {
    val postCode = "AB12CD"
    val expected = "AB1 2CD"
    val result = FMNHelper.splitPostCode(postCode)
    result shouldEqual expected
  }

  it should "handle postcodes without spaces" in {
    val postCode = "E11AA"
    val expected = "E1 1AA"
    val result = FMNHelper.splitPostCode(postCode)
    result shouldEqual expected
  }

  it should "handle postcodes with spaces" in {
    val postCode = " SW1A 2AA "
    val expected = "SW1A 2AA"
    val result = FMNHelper.splitPostCode(postCode)
    result shouldEqual expected
  }

  "createNPSFMNRequest" should "return NPSFMNRequest with data when idData is defined" in {
    val idData = Some(IndividualDetailsDataCache(
      id="id",
      individualDetails = Some(IndividualDetailsData(
        firstForename = "John",
        surname = "Doe",
        dateOfBirth = "1990-01-01",
        postCode = "AB12CD",
        nino = "AB123456C"
      ))))

    val expected = NPSFMNRequest(
      firstForename = "John",
      surname = "Doe",
      dateOfBirth = "1990-01-01",
      postCode = "AB12CD"
    )
    val result = FMNHelper.createNPSFMNRequest(idData)
    result shouldEqual expected
  }

  it should "return empty NPSFMNRequest when idData is None" in {
    val idData = None
    val expected = NPSFMNRequest.empty
    val result = FMNHelper.createNPSFMNRequest(idData)
    result shouldEqual expected
  }

  it should "return empty NPSFMNRequest when individualDetails is None" in {
    val idData = Some(IndividualDetailsDataCache(id="id",
      individualDetails = None
    ))
    val expected = NPSFMNRequest.empty
    val result = FMNHelper.createNPSFMNRequest(idData)
    result shouldEqual expected
  }


}