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
import models.requests.{IdentifierRequest, OptionalDataRequest}
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

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {
  private val dummyValue               = "ZZ11ZZ"
  private val userAnswers: UserAnswers = UserAnswers().setOrException(ConfirmYourPostcodePage, dummyValue)
  class HarnessNoOrigin(sessionRepository: SessionRepository) extends DataRetrievalImpl(sessionRepository, None) {
    def callTransform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }
  class HarnessWithOrigin(sessionRepository: SessionRepository, optOrigin: Option[OriginType] = Some(OriginType.FMN))
      extends DataRetrievalImpl(sessionRepository, optOrigin) {
    def callTransform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" - {
    "when there is data in the cache and no origin passed in" - {
      "if session data new format must add user answers and origin to the request and not update cache" in {
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get("id")) thenReturn Future(
          Some(SessionData(userAnswers = userAnswers, origin = Some(OriginType.PDV), id = "id"))
        )
        val action            = new HarnessNoOrigin(sessionRepository)
        val result            = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.flatMap(_.get(ConfirmYourPostcodePage)) mustBe Some(dummyValue)
        result.origin mustBe Some(OriginType.PDV)
        verify(sessionRepository, never).set(any())
      }
      "if session data old format must add user answers and origin to the request and update cache" in {
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get("id")) thenReturn Future(
          Some(SessionData(userAnswers = userAnswers, origin = Some(OriginType.PDV), id = "id", isOldFormat = true))
        )
        when(sessionRepository.set(any())) thenReturn Future(true)
        val action            = new HarnessNoOrigin(sessionRepository)
        val result            = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.flatMap(_.get(ConfirmYourPostcodePage)) mustBe Some(dummyValue)
        result.origin mustBe Some(OriginType.PDV)
        verify(sessionRepository, times(1)).set(any())
      }
    }

    "when there is data in the cache and an origin passed in" - {
      "must keep existing user answers and update origin to the new origin" in {
        val sessionRepository                              = mock[SessionRepository]
        when(sessionRepository.get("id")) thenReturn Future(
          Some(SessionData(userAnswers = userAnswers, origin = Some(OriginType.PDV), id = "id"))
        )
        when(sessionRepository.set(any())) thenReturn Future(true)
        val action                                         = new HarnessWithOrigin(sessionRepository)
        val sessionDataCaptor: ArgumentCaptor[SessionData] = ArgumentCaptor.forClass(classOf[SessionData])
        val result                                         = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.flatMap(_.get(ConfirmYourPostcodePage)) mustBe Some(dummyValue)
        result.origin mustBe Some(OriginType.FMN)
        verify(sessionRepository, times(1)).set(sessionDataCaptor.capture())
        val actualSessionDataUpdated = sessionDataCaptor.getValue
        actualSessionDataUpdated.userAnswers mustBe userAnswers
        actualSessionDataUpdated.origin mustBe Some(OriginType.FMN)
      }
    }

    "when there is no data in the cache and an origin passed in" - {
      "must create a new user empty answers with specified origin and save it in Mongo" in {
        val sessionRepository                              = mock[SessionRepository]
        when(sessionRepository.get("id")) thenReturn Future(None)
        when(sessionRepository.set(any())) thenReturn Future(true)
        val action                                         = new HarnessWithOrigin(sessionRepository)
        val sessionDataCaptor: ArgumentCaptor[SessionData] = ArgumentCaptor.forClass(classOf[SessionData])
        val result                                         = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.flatMap(_.get(ConfirmYourPostcodePage)) mustBe None
        result.origin mustBe Some(OriginType.FMN)
        verify(sessionRepository, times(1)).set(sessionDataCaptor.capture())
        val actualSessionDataUpdated = sessionDataCaptor.getValue
        actualSessionDataUpdated.userAnswers mustBe UserAnswers()
        actualSessionDataUpdated.origin mustBe Some(OriginType.FMN)
      }
    }

    "when there is no data in the cache and the UNKNOWN origin passed in" - {
      "must create a new user empty answers with UNKNOWN origin but NOT save it in Mongo" in {
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get("id")) thenReturn Future(None)
        when(sessionRepository.set(any())) thenReturn Future(true)
        val action            = new HarnessWithOrigin(sessionRepository, Some(OriginType.Unknown))
        val result            = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.flatMap(_.get(ConfirmYourPostcodePage)) mustBe None
        result.origin mustBe Some(OriginType.Unknown)
        verify(sessionRepository, never).set(any)
      }
    }

    "when there is no data in the cache and no origin passed in" - {
      "must create a new user empty answers with no origin" in {
        val sessionRepository = mock[SessionRepository]
        when(sessionRepository.get("id")) thenReturn Future(None)
        when(sessionRepository.set(any())) thenReturn Future(true)
        val action            = new HarnessNoOrigin(sessionRepository)
        val result            = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers.flatMap(_.get(ConfirmYourPostcodePage)) mustBe None
        result.origin mustBe None
        verify(sessionRepository, never).set(any)
      }
    }
  }
}
