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

package repositories

import config.FrontendAppConfig
import models.requests.DataRequest
import models.{OriginType, SessionData, UserAnswers}
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class SessionRepositoryISpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[SessionData]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant          = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val userAnswers = UserAnswers(Json.obj("foo" -> "bar"))
  private val sessionData = SessionData(userAnswers, Some(OriginType.PDV), Instant.ofEpochSecond(1), "id")
  private val request     = DataRequest(FakeRequest(), "id", userAnswers, Some("credid-01234"), Some(OriginType.FMN))

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1L

  protected override val repository: SessionRepository = new SessionRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      val expectedResult = sessionData copy (lastUpdated = instant)

      val setResult     = repository.set(sessionData).futureValue
      val updatedRecord = find(Filters.equal("_id", sessionData.id)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual expectedResult
    }
  }

  ".setUserAnswers" - {
    "must set the last updated time to now or greater and the user answers and save them" in {
      val expectedResult = sessionData.copy(lastUpdated = instant, origin = Some(OriginType.PDV))

      insert(sessionData).futureValue

      val setResult     = repository.setUserAnswers("id", userAnswers).futureValue
      val updatedRecord = find(Filters.equal("_id", sessionData.id)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord.id mustEqual expectedResult.id
      updatedRecord.origin mustEqual expectedResult.origin
      updatedRecord.userAnswers mustEqual expectedResult.userAnswers
      updatedRecord.lastUpdated.compareTo(instant) >= 0 mustBe true
    }

  }

  ".get" - {

    "when there is a record for this id" - {

      "must update the lastUpdated time and get the record" in {

        insert(sessionData).futureValue

        val result         = repository.get(sessionData.id).futureValue
        val expectedResult = sessionData copy (lastUpdated = instant)

        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.get("id that does not exist").futureValue must not be defined
      }
    }
  }

  ".clear" - {

    "must remove a record" in {

      insert(sessionData).futureValue

      val result = repository.clear(sessionData.id).futureValue

      result mustEqual true
      repository.get(sessionData.id).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist").futureValue

      result mustEqual true
    }
  }

  ".keepAlive" - {

    "when there is a record for this id" - {

      "must update its lastUpdated to `now` and return true" in {

        insert(sessionData).futureValue

        val result = repository.keepAlive(sessionData.id).futureValue

        val expectedUpdatedAnswers = sessionData copy (lastUpdated = instant)

        result mustEqual true
        val updatedAnswers = find(Filters.equal("_id", sessionData.id)).futureValue.headOption.value
        updatedAnswers mustEqual expectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {

      "must return true" in {

        repository.keepAlive("id that does not exist").futureValue mustEqual true
      }
    }
  }
}
