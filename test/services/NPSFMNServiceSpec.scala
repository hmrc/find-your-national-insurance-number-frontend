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

package services

import config.FrontendAppConfig
import connectors.DefaultNPSFMNConnector
import models.nps.NPSFMNRequest
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.mock
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future
import scala.util.Random

class NPSFMNServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach{

  import NPSFMNServiceSpec._
  override def beforeEach(): Unit = {
    reset(mockConnector, mockFrontendAppConfig)
  }

  "NPSFMNServiceImpl.sendLetter" must {

    "sendLetter return true when connector returns 200" ignore {
      when(mockConnector.sendLetter(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, "")))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe true
      }
    }

    "sendLetter return false when connector returns 400" ignore {
      when(mockConnector.sendLetter(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(400, "")))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe false
      }

    }

    "sendLetter return false when connector returns 500" ignore {
      when(mockConnector.sendLetter(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(500, "")))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe false
      }
    }

    "sendLetter return false when connector returns 404" ignore {
      when(mockConnector.sendLetter(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(404, "")))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe false
      }
    }

  }

}

object NPSFMNServiceSpec {
  private val mockConnector = mock[DefaultNPSFMNConnector]
  private val mockFrontendAppConfig = mock[FrontendAppConfig]

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val npsFMNService = new NPSFMNServiceImpl(mockConnector, mockFrontendAppConfig)(ec)
  val fakeNino: Nino = Nino(new Generator(new Random()).nextNino.nino)
  val fakeNPSRequest: NPSFMNRequest = NPSFMNRequest("test", "test", "test", "test")
}
