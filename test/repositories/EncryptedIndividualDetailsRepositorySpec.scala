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

import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import config.FrontendAppConfig
import models.encryption.id.EncryptedIndividualDetailsDataCache
import models.individualdetails.{IndividualDetailsData, IndividualDetailsDataCache}
import org.scalatest.OptionValues
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class EncryptedIndividualDetailsRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EncryptedIndividualDetailsDataCache]
    with ScalaFutures
    with OptionValues
    with IntegrationPatience
    with MockitoSugar {

  private val mockAppConfig = mock[FrontendAppConfig]

  when(mockAppConfig.cacheTtl) thenReturn 1
  when(mockAppConfig.encryptionKey) thenReturn "z4rWoRLf7a1OHTXLutSDJjhrUzZTBE3b"


  protected val repository = new EncryptedIndividualDetailsRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig
  )


  "EncryptedIndividualDetailsRepository" - {

    "insertOrReplaceIndividualDetailsData" - {

      "must insert or replace the IndividualDetailsData" in {

        val individualDetailsDataCache: IndividualDetailsDataCache = IndividualDetailsDataCache(
          id = "id",
          individualDetails = Some(IndividualDetailsData("John", "Doe", "1980-01-01", "AB12CD", "AB123456C"))
        )
        val result = repository.insertOrReplaceIndividualDetailsData(individualDetailsDataCache).futureValue
        result mustBe "AB123456C"
      }
    }

    "findIndividualDetailsDataByNino" - {

      val individualDetailsDataCache: IndividualDetailsDataCache = IndividualDetailsDataCache(
        id = "id",
        individualDetails = Some(IndividualDetailsData("John", "Doe", "1980-01-01", "AB12CD", "AB123456C")),
        lastUpdated = Instant.EPOCH
      )

      "must return the IndividualDetailsData when it exists" in {

        val nino = "AB123456C"

        val result1 = repository.insertOrReplaceIndividualDetailsData(individualDetailsDataCache).futureValue
        result1 mustBe "AB123456C"

        val result = repository.findIndividualDetailsDataByNino(nino).futureValue
        result.value.copy(lastUpdated = Instant.EPOCH) mustEqual individualDetailsDataCache.copy(lastUpdated = Instant.EPOCH)
      }

      "must return None when the IndividualDetailsData does not exist" in {
        val nino = "ZZ999999Z"
        val result = repository.findIndividualDetailsDataByNino(nino).futureValue
        result mustBe None
      }
    }
  }
}