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

package services

import config.FrontendAppConfig
import connectors.DefaultNPSFMNConnector
import models.CorrelationId
import models.nps._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NOT_IMPLEMENTED}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import util.AnyValueTypeMatcher.anyValueType

import scala.concurrent.Future
import scala.util.Random

class NPSFMNServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  import NPSFMNServiceSpec._

  override def beforeEach(): Unit = {
    reset(mockConnector, mockFrontendAppConfig)
    when(mockFrontendAppConfig.npsFMNAppStatusMessageList).thenReturn("63471,63472,63473")
    ()
  }

  "NPSFMNServiceImpl.sendLetter" must {

    "return Letter issued response when connector returns 202" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(ACCEPTED, "")))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe LetterIssuedResponse()
      }
    }

    "return TechnicalIssueResponse with NOT_FOUND when connector returns 404" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, NotfoundObject)))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe TechnicalIssueResponse(NOT_FOUND, "MATCHING_RESOURCE_NOT_FOUND")
      }

    }

    "return TechnicalIssueResponse with INTERNAL_SERVER_ERROR when connector returns 500" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, InternalServerErrorObject)))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe TechnicalIssueResponse(INTERNAL_SERVER_ERROR, "GENERIC_SERVER_ERROR")
      }
    }

    "return TechnicalIssueResponse with NOT_IMPLEMENTED when connector returns 501" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(NOT_IMPLEMENTED, NotImplementedObject)))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe TechnicalIssueResponse(NOT_IMPLEMENTED, "NOT_IMPLEMENTED")
      }
    }

    "return FailureResponse when connector returns 400 and response body contains 'failures'" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, failureResponseBody)))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe FailureResponse(List(Failure("some type", "some reason")))
      }
    }

    "return RLSDLONFAResponse when connector returns 400 and response body does not contain 'failures' but passes check" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, RLSDLONFAResponseBody)))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe RLSDLONFAResponse(BAD_REQUEST, "BAD_REQUEST")
      }
    }

    "return TechnicalIssueResponse when connector returns 400 and response body does not contain 'failures' and does not pass check" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, someOtherErrorResponseBody)))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe TechnicalIssueResponse(BAD_REQUEST, "SOME_OTHER_ERROR")
      }
    }

    "return TechnicalIssueResponse when response body does not contain 'failures' and there are no messages" in {
      when(mockConnector.sendLetter(any(), any())(any(), anyValueType[CorrelationId], any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, someOtherErrorResponseBody)))

      npsFMNService.sendLetter(fakeNino.nino, fakeNPSRequest).map { result =>
        result mustBe TechnicalIssueResponse(BAD_REQUEST, "SOME_OTHER_ERROR")
      }
    }

  }

}

object NPSFMNServiceSpec {
  private val mockConnector         = mock[DefaultNPSFMNConnector]
  private val mockFrontendAppConfig = mock[FrontendAppConfig]

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val hc: HeaderCarrier                     = HeaderCarrier()

  val npsFMNService                 = new NPSFMNServiceImpl(mockConnector, mockFrontendAppConfig)(ec)
  val fakeNino: Nino                = Nino(new Generator(new Random()).nextNino.nino)
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

  val RLSDLONFAResponseBody =
    """
      {
        "origin": "HoD",
        "response": {
          "jsonServiceError": {
            "requestURL": "/itmp/find-my-nino/api/v1/individual/AA123456A",
            "message": "BAD_REQUEST",
            "appStatusMessageCount": 1,
            "appStatusMessageList": {
              "appStatusMessage": [
                "63472"
              ]
            }
          }
        }
      }
    """

  val someOtherErrorResponseBody =
    """
      {
        "origin": "HIP",
        "response": {
          "jsonServiceError": {
            "requestURL": "/itmp/find-my-nino/api/v1/individual/AA123456A",
            "message": "SOME_OTHER_ERROR",
            "appStatusMessageCount": 1,
            "appStatusMessageList": {
              "appStatusMessage": [

              ]
            }
          }
        }
      }
    """
  val failureResponseBody        =
    """
      {
        "origin": "HIP",
        "response": {
          "failures": [
            {
              "type": "some type",
              "reason": "some reason"
            }
          ]
        }
      } """
}
