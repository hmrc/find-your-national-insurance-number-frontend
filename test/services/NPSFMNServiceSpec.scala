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
import models.CorrelationId
import models.nps.{LetterIssuedResponse, NPSFMNRequest, TechnicalIssueResponse}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.mock
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.AnyValueTypeMatcher.anyValueType

import scala.concurrent.Future
import scala.util.Random

class NPSFMNServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach{

  import NPSFMNServiceSpec._
  override def beforeEach(): Unit = {
    reset(mockConnector, mockFrontendAppConfig)
  }

  "NPSFMNServiceImpl.sendLetter" must {

    "sendLetter return true when connector returns 202" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(202, "")))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe LetterIssuedResponse()
      }
    }

    "sendLetter return false when connector returns 404" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(404, NotfoundObject)))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe TechnicalIssueResponse(404, "MATCHING_RESOURCE_NOT_FOUND")
      }

    }

    "sendLetter return false when connector returns 500" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(500, InternalServerErrorObject)))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe TechnicalIssueResponse(500, "GENERIC_SERVER_ERROR")
      }
    }

    "sendLetter return false when connector returns 501" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(501, NotImplementedObject)))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe TechnicalIssueResponse(501, "NOT_IMPLEMENTED")
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

  val NotfoundObject: String =
    s"""
       |{
       | "origin": "HoD",
       | "response": {
       |  "jsonServiceError": {
       |    "requestURL": "/itmp/find-my-nino/api/v1/individual/AA123456A",
       |    "message": "MATCHING_RESOURCE_NOT_FOUND",
       |    "appStatusMessageCount": 1,
       |    "appStatusMessageList": {
       |      "appStatusMessage": [
       |        "Uri does not exist"
       |      ]
       |    }
       |  }
       |  }
       |}
       |""".stripMargin

  val InternalServerErrorObject: String =
    s"""
       |{
       | "origin": "HoD",
       | "response": {
       |  "jsonServiceError": {
       |    "requestURL": "/itmp/find-my-nino/api/v1/individual/AA123456A",
       |    "message": "GENERIC_SERVER_ERROR",
       |    "appStatusMessageCount": 1,
       |    "appStatusMessageList": {
       |      "appStatusMessage": [
       |        "Internal Server Error"
       |      ]
       |    }
       |  }
       |  }
       |}
       |""".stripMargin

  val NotImplementedObject: String =
    s"""
       |{
       |  "jsonServiceError": {
       |    "requestURL": "/itmp/find-my-nino/api/v1/individual/AA123456A",
       |    "message": "NOT_IMPLEMENTED",
       |    "appStatusMessageCount": 1,
       |    "appStatusMessageList": {
       |      "appStatusMessage": [
       |        "Context determined but code is not active"
       |      ]
       |    }
       |  }
       |}
       |""".stripMargin
}
