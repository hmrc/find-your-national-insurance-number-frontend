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
import models.UserAnswers
import models.pdv.{DataRequestWithOptionalUserAnswers, PDVNotFoundResponse}
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.NOT_FOUND
import play.api.test.FakeRequest
import repositories.SessionRepository
import services.PersonalDetailsValidationService
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  class Harness(sessionRepository: SessionRepository) extends DataRetrievalActionImpl(sessionRepository, mockPersonalDetailsValidationService) {
    def callTransform[A](request: IdentifierRequest[A]): Future[DataRequestWithOptionalUserAnswers[A]] = transform(request)
  }

  "Data Retrieval Action" - {

    "when there is no data in the cache" - {

      "must set userAnswers to 'None' in the request" in {

        when(mockSessionRepository.get("id")) thenReturn Future(None)

        val pdvNotFoundResponse = PDVNotFoundResponse(HttpResponse(NOT_FOUND, "PDV data not found"))

        when(mockPersonalDetailsValidationService.getPDVMatchResult(any())(any())).thenReturn(Future.successful(pdvNotFoundResponse))

        val action = new Harness(mockSessionRepository)

        val result = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers must not be defined
      }
    }

    "when there is data in the cache" - {

      "must build a userAnswers object and add it to the request" in {

        when(mockSessionRepository.get("id")).thenReturn(Future(Some(UserAnswers("id"))))

        val pdvNotFoundResponse = PDVNotFoundResponse(HttpResponse(NOT_FOUND, "PDV data not found"))

        when(mockPersonalDetailsValidationService.getPDVMatchResult(any())(any())).thenReturn(Future.successful(pdvNotFoundResponse))

        val action = new Harness(mockSessionRepository)

        val result = action.callTransform(IdentifierRequest(FakeRequest(), "id", Some("credid-01234"))).futureValue

        result.userAnswers mustBe defined
      }
    }
  }
}
