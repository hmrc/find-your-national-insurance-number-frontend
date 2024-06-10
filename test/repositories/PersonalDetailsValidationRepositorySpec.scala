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

package repositories

import config.FrontendAppConfig
import models.pdv.{PDVResponseData, PersonalDetails, ValidationStatus}
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Instant, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global

class PersonalDetailsValidationRepositorySpec
  extends AnyFreeSpec
  with Matchers
  with DefaultPlayMongoRepositorySupport[PDVResponseData]
  with ScalaFutures
  with IntegrationPatience
  with OptionValues
  with MockitoSugar {

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1
  when(mockAppConfig.encryptionKey) thenReturn "z4rWoRLf7a1OHTXLutSDJjhrUzZTBE3b"

  private val instant = Instant.ofEpochSecond(1234567890)

  protected override val repository = new PersonalDetailsValidationRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig
  )

  "PersonalDetailsValidationRepository" - {

    "insertOrReplacePDVResultData" - {

      "must insert or replace the PDVResultData" in {
        val pdvResultData = PDVResponseData(
          id = "id",
          validationStatus = ValidationStatus.Success,
          personalDetails = Some(PersonalDetails(
            firstName = "firstName",
            lastName = "lastName",
            nino = Nino("AB123456C"),
            postCode = Some("postcode"),
            dateOfBirth = LocalDate.now()
          )),
          lastUpdated = instant,
          None, None, None, None
        )

        val result = repository.insertOrReplacePDVResultData(pdvResultData).futureValue
        result mustBe "AB123456C"
      }

      "must insert or replace the PDVResultData when the PDVResultData already exists" in {
        val pdvResultData = PDVResponseData(
          id = "id",
          validationStatus = ValidationStatus.Success,
          personalDetails = Some(PersonalDetails(
            firstName = "firstName",
            lastName = "lastName",
            nino = Nino("AB123456C"),
            postCode = Some("postcode"),
            dateOfBirth = LocalDate.now()
          )),
          lastUpdated = instant,
          None, None, None, None
        )

        val result = repository.insertOrReplacePDVResultData(pdvResultData).futureValue
        result mustBe "AB123456C"

        val pdvResultData2 = PDVResponseData(
          id = "id",
          validationStatus = ValidationStatus.Success,
          personalDetails = Some(PersonalDetails(
            firstName = "firstName",
            lastName = "lastName",
            nino = Nino("AB123456C"),
            postCode = Some("postcode2"),
            dateOfBirth = LocalDate.now()
          )),
          lastUpdated = instant,
          None, None, None, None
        )

        val result2 = repository.insertOrReplacePDVResultData(pdvResultData2).futureValue
        result2 mustBe "AB123456C"

      }

    }

    "updateCustomerValidityWithReason" - {

      "must update the customer validity and reason" in {
        val pdvResultData = PDVResponseData(
          id = "id",
          validationStatus = ValidationStatus.Success,
          personalDetails = Some(PersonalDetails(
            firstName = "firstName",
            lastName = "lastName",
            nino = Nino("AB123456C"),
            postCode = Some("postcode"),
            dateOfBirth = LocalDate.now()
          )),
          lastUpdated = instant,
          None, None, None, None
        )

        val result = repository.insertOrReplacePDVResultData(pdvResultData).futureValue
        result mustBe "AB123456C"

        val result2 = repository.updateCustomerValidityWithReason("AB123456C", validCustomer = true, "reason").futureValue
        result2 mustBe "AB123456C"
      }
    }

    "updatePDVDataWithNPSPostCode" - {

      "must update the npsPostCode" in {
        val pdvResultData = PDVResponseData(
          id = "id",
          validationStatus = ValidationStatus.Success,
          personalDetails = Some(PersonalDetails(
            firstName = "firstName",
            lastName = "lastName",
            nino = Nino("AB123456C"),
            postCode = Some("postcode"),
            dateOfBirth = LocalDate.now()
          )),
          lastUpdated = instant,
          None, None, None, None
        )

        val result = repository.insertOrReplacePDVResultData(pdvResultData).futureValue
        result mustBe "AB123456C"

        val result2 = repository.updatePDVDataWithNPSPostCode("AB123456C", "npsPostCode").futureValue
        result2 mustBe "AB123456C"
      }
    }

    "findByValidationId" - {

        "must return the PDVResultData when it exists" in {
          val pdvResultData = PDVResponseData(
            id = "id",
            validationStatus = ValidationStatus.Success,
            personalDetails = Some(PersonalDetails(
              firstName = "firstName",
              lastName = "lastName",
              nino = Nino("AB123456C"),
              postCode = Some("postcode"),
              dateOfBirth = LocalDate.of(2010, 1, 1)
            )),
            lastUpdated = instant,
            None, None, None, None
          )

          val result = repository.insertOrReplacePDVResultData(pdvResultData).futureValue
          result mustBe "AB123456C"

          val result2 = repository.findByValidationId("id").futureValue
          result2.value.personalDetails mustBe pdvResultData.personalDetails
          result2.value.validationStatus mustBe pdvResultData.validationStatus
          result2.value.npsPostCode mustBe pdvResultData.npsPostCode
        }

        "must return None when the PDVResultData does not exist" in {
          val result = repository.findByValidationId("id").futureValue
          result mustBe None
        }
    }

    "findByNino" - {

        "must return the PDVResultData when it exists" in {
          val pdvResultData = PDVResponseData(
            id = "id",
            validationStatus = ValidationStatus.Success,
            personalDetails = Some(PersonalDetails(
              firstName = "firstName",
              lastName = "lastName",
              nino = Nino("AB123456C"),
              postCode = Some("postcode"),
              dateOfBirth = LocalDate.now()
            )),
            lastUpdated = instant,
            None, None, None, None
          )

          val result = repository.insertOrReplacePDVResultData(pdvResultData).futureValue
          result mustBe "AB123456C"

          val result2 = repository.findByNino("AB123456C").futureValue
          result2.value.personalDetails mustBe pdvResultData.personalDetails
          result2.value.validationStatus mustBe pdvResultData.validationStatus
          result2.value.npsPostCode mustBe pdvResultData.npsPostCode
        }

        "must return None when the PDVResultData does not exist" in {
          val result = repository.findByNino("AB123456C").futureValue
          result mustBe None
        }
    }

  }

}
