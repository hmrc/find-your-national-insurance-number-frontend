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
import models.TryAgainCount
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.global

class TryAgainCountRepositorySpec extends AnyFreeSpec
  with Matchers
  with DefaultPlayMongoRepositorySupport[TryAgainCount]
  with ScalaFutures
  with IntegrationPatience
  with OptionValues
  with MockitoSugar {

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1

  private val instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  protected override val repository = new TryAgainCountRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock
  )(global)

  "TryAgainCountRepository" - {

    "insertOrIncrement" - {

      "must insert or increment the TryAgainCount" in {
        val tryAgainCount = TryAgainCount(
          id = "id",
          count = 1,
          lastUpdated = instant
        )

        val result = repository.insertOrIncrement(tryAgainCount.id)(global).futureValue
        result mustBe true
      }
    }

    "set" - {

        "must set the TryAgainCount" in {
          val tryAgainCount = TryAgainCount(
            id = "id",
            count = 1,
            lastUpdated = instant
          )

          val result = repository.set(tryAgainCount)(global).futureValue
          result mustBe true
        }
    }

    "insert" - {

        "must insert the TryAgainCount" in {
          val tryAgainCount = TryAgainCount(
            id = "id",
            count = 1,
            lastUpdated = instant
          )

          val result = repository.insert(tryAgainCount)(global).futureValue
          result mustBe true
        }
    }

    "findById" - {

        "must find the TryAgainCount" in {
          val tryAgainCount = TryAgainCount(
            id = "id",
            count = 1,
            lastUpdated = instant
          )

          repository.insertOrIncrement(tryAgainCount.id)(global).futureValue

          val result = repository.findById(tryAgainCount.id)(global).futureValue
          result.get.id mustBe tryAgainCount.id
        }
    }

  }

}
