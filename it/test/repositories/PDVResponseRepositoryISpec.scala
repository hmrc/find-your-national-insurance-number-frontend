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
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class PDVResponseRepositoryISpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[PDVResponseData]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val fakeNino = Nino(new Generator(new Random()).nextNino.nino)

  val personalDetails: PersonalDetails =
    PersonalDetails(
      "firstName",
      "lastName",
      fakeNino,
      Some("AA1 1AA"),
      LocalDate.parse("1945-03-18")
    )

  private val pdvResponseData = PDVResponseData(
    "id", ValidationStatus.Success, Some(personalDetails), Instant.ofEpochSecond(1), None, Some(false), Some("false"),Some("somePostcode"))

  private val validCustomerPDVResponseData = PDVResponseData(
    "id", ValidationStatus.Success, Some(personalDetails), Instant.ofEpochSecond(1), Some("Valid Reason"), Some(true), Some("false"),Some("somePostcode"))

  private val invalidCustomerPDVResponseData = PDVResponseData(
    "id", ValidationStatus.Success, Some(personalDetails), Instant.ofEpochSecond(1), Some("Invalid Reason"), Some(false), Some("false"),Some("somePostcode"))

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1
  when(mockAppConfig.encryptionKey) thenReturn "z4rWoRLf7a1OHTXLutSDJjhrUzZTBE3b"

  protected override val repository = new PersonalDetailsValidationRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
  )


  ".updateCustomerValidityWithReason" - {

    "when there is a record for this id" - {

      "must update record to be a valid customer" in {

        val expectedResult = validCustomerPDVResponseData copy (lastUpdated = instant)

        insert(pdvResponseData).futureValue

        repository.updateCustomerValidityWithReason(pdvResponseData.getNino, validCustomer = true, "Valid Reason").futureValue

        val result = repository.findByValidationId(pdvResponseData.id).futureValue

        result.value.validationStatus mustEqual expectedResult.validationStatus
        result.value.personalDetails  mustEqual expectedResult.personalDetails
        result.value.reason           mustEqual expectedResult.reason
      }

      "must update record to be an invalid customer" in {

        val expectedResult = invalidCustomerPDVResponseData copy (lastUpdated = instant)

        insert(pdvResponseData).futureValue

        repository.updateCustomerValidityWithReason(pdvResponseData.getNino, validCustomer = false, "Invalid Reason").futureValue

        val result = repository.findByValidationId(pdvResponseData.id).futureValue

        result.value.validationStatus mustEqual expectedResult.validationStatus
        result.value.personalDetails  mustEqual expectedResult.personalDetails
        result.value.reason           mustEqual expectedResult.reason
      }
    }
  }

  ".findByValidationId" - {

    "when there is a record for this id" - {

      "must get the record" in {

        insert(pdvResponseData).futureValue

        val result = repository.findByValidationId(pdvResponseData.id).futureValue
        val expectedResult = pdvResponseData

        result.value.id               mustEqual expectedResult.id
        result.value.personalDetails  mustEqual expectedResult.personalDetails
        result.value.validationStatus mustEqual expectedResult.validationStatus
      }
    }

    "when there is no record for this id" - {

      "must return None" in {
        repository.findByValidationId("id that does not exist").futureValue must not be defined
      }
    }
  }

  ".findByNino" - {

    "when there is a record for this id" - {

      "must get the record" in {

        insert(pdvResponseData).futureValue

        val result = repository.findByNino(pdvResponseData.personalDetails.get.nino.value).futureValue
        val expectedResult = pdvResponseData

        result.value.id mustEqual expectedResult.id
        result.value.personalDetails mustEqual expectedResult.personalDetails
        result.value.validationStatus mustEqual expectedResult.validationStatus
      }
    }

    "when there is no record for this id" - {

      "must return None" in {
        repository.findByValidationId("id that does not exist").futureValue must not be defined
      }
    }
  }
}
