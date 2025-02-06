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

package controllers.actions

import base.SpecBase
import models.requests.{DataRequest, IdentifierRequest}
import models.{OriginType, SessionData, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import pages.ConfirmYourPostcodePage
import play.api.test.FakeRequest
import repositories.SessionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalSpec extends SpecBase with MockitoSugar {
  private val dummyValue               = "ZZ11ZZ"
  private val userAnswers: UserAnswers = UserAnswers().setOrException(ConfirmYourPostcodePage, dummyValue)

  private class Harness(sessionRepository: SessionRepository, optOrigin: Option[OriginType])
      extends DataRetrievalImpl(sessionRepository, optOrigin) {
    def callTransform[A](request: IdentifierRequest[A]): Future[DataRequest[A]] = transform(request)
  }

  "transform" - {
    "when there is data in the cache and no origin passed in" - {
      "must NOT update cache" in {
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get(any)) thenReturn Future(
          Some(SessionData(userAnswers = userAnswers, origin = Some(OriginType.PDV), id = "id"))
        )
        when(sessionRepository.set(any())) thenReturn Future(true)
        val action            = new Harness(sessionRepository, None)
        val result            = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.get(ConfirmYourPostcodePage) mustBe Some(dummyValue)
        result.origin mustBe Some(OriginType.PDV)
        verify(sessionRepository, never).set(any)
      }

      "if no origin in cache must NOT update cache" in {
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get(any)) thenReturn Future(
          Some(SessionData(userAnswers = userAnswers, origin = None, id = "id"))
        )
        val action            = new Harness(sessionRepository, None)
        val result            = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.get(ConfirmYourPostcodePage) mustBe Some(dummyValue)
        result.origin mustBe None
        verify(sessionRepository, never).set(any)
      }

      "if no origin in cache but session data old format must update cache" in {
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get(any)) thenReturn Future(
          Some(SessionData(userAnswers = userAnswers, origin = None, id = "id", isOldFormat = true))
        )
        when(sessionRepository.set(any())) thenReturn Future(true)
        val action            = new Harness(sessionRepository, None)
        val result            = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.get(ConfirmYourPostcodePage) mustBe Some(dummyValue)
        result.origin mustBe None
        val sessionDataCaptor: ArgumentCaptor[SessionData] = ArgumentCaptor.forClass(classOf[SessionData])
        verify(sessionRepository, times(1)).set(sessionDataCaptor.capture())
        val actualSessionDataUpdated                       = sessionDataCaptor.getValue
        actualSessionDataUpdated.userAnswers mustBe userAnswers
        actualSessionDataUpdated.origin mustBe None
      }
    }

    "when there is data in the cache and an origin passed in" - {
      "if there is a different origin in cache must update origin to the new one" in {
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get(any)) thenReturn Future(
          Some(SessionData(userAnswers = userAnswers, origin = Some(OriginType.PDV), id = "id"))
        )
        when(sessionRepository.set(any())) thenReturn Future(true)
        val action            = new Harness(sessionRepository, Some(OriginType.FMN))
        val result            = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.get(ConfirmYourPostcodePage) mustBe Some(dummyValue)
        result.origin mustBe Some(OriginType.FMN)
        val sessionDataCaptor: ArgumentCaptor[SessionData] = ArgumentCaptor.forClass(classOf[SessionData])
        verify(sessionRepository, times(1)).set(sessionDataCaptor.capture())
        val actualSessionDataUpdated                       = sessionDataCaptor.getValue
        actualSessionDataUpdated.userAnswers mustBe userAnswers
        actualSessionDataUpdated.origin mustBe Some(OriginType.FMN)
      }

      "if there is no origin in cache must update origin to the new one" in {
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get(any)) thenReturn Future(
          Some(SessionData(userAnswers = userAnswers, origin = None, id = "id"))
        )
        when(sessionRepository.set(any())) thenReturn Future(true)
        val action            = new Harness(sessionRepository, Some(OriginType.FMN))
        val result            = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.get(ConfirmYourPostcodePage) mustBe Some(dummyValue)
        result.origin mustBe Some(OriginType.FMN)
        val sessionDataCaptor: ArgumentCaptor[SessionData] = ArgumentCaptor.forClass(classOf[SessionData])
        verify(sessionRepository, times(1)).set(sessionDataCaptor.capture())
        val actualSessionDataUpdated                       = sessionDataCaptor.getValue
        actualSessionDataUpdated.userAnswers mustBe userAnswers
        actualSessionDataUpdated.origin mustBe Some(OriginType.FMN)
      }

      "if the same origin is in cache must NOT update origin" in {
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get(any)) thenReturn Future(
          Some(SessionData(userAnswers = userAnswers, origin = Some(OriginType.FMN), id = "id"))
        )
        val action            = new Harness(sessionRepository, Some(OriginType.FMN))
        val result            = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.get(ConfirmYourPostcodePage) mustBe Some(dummyValue)
        result.origin mustBe Some(OriginType.FMN)
        verify(sessionRepository, never).set(any)
      }
    }

    "when there is NO data in the cache and an origin passed in" - {
      "must create a new user empty answers with specified origin and save it in Mongo" in {
        val sessionRepository                              = mock[SessionRepository]
        when(sessionRepository.get(any)) thenReturn Future(None)
        when(sessionRepository.set(any())) thenReturn Future(true)
        val action                                         = new Harness(sessionRepository, Some(OriginType.FMN))
        val sessionDataCaptor: ArgumentCaptor[SessionData] = ArgumentCaptor.forClass(classOf[SessionData])
        val result                                         = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.get(ConfirmYourPostcodePage) mustBe None
        result.origin mustBe Some(OriginType.FMN)
        verify(sessionRepository, times(1)).set(sessionDataCaptor.capture())
        val actualSessionDataUpdated = sessionDataCaptor.getValue
        actualSessionDataUpdated.userAnswers mustBe UserAnswers()
        actualSessionDataUpdated.origin mustBe Some(OriginType.FMN)
      }
    }

    "when there is NO data in the cache and NO origin passed in" - {
      "must create a new user empty answers with no origin and save it in Mongo" in {
        val sessionRepository                              = mock[SessionRepository]
        when(sessionRepository.get(any)) thenReturn Future(None)
        when(sessionRepository.set(any())) thenReturn Future(true)
        val action                                         = new Harness(sessionRepository, None)
        val sessionDataCaptor: ArgumentCaptor[SessionData] = ArgumentCaptor.forClass(classOf[SessionData])
        val result                                         = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.get(ConfirmYourPostcodePage) mustBe None
        result.origin mustBe None
        verify(sessionRepository, times(1)).set(sessionDataCaptor.capture())
        val actualSessionDataUpdated = sessionDataCaptor.getValue
        actualSessionDataUpdated.userAnswers mustBe UserAnswers()
        actualSessionDataUpdated.origin mustBe None
      }
    }
  }
}
