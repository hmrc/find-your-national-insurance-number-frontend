/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import com.codahale.metrics.{Counter, Meter, MetricRegistry, Timer}
import models.IndividualDetailsResponseEnvelope
import models.errors.{ConnectorError, IndividualDetailsError}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsResult, JsSuccess, JsValue, Json, Reads}
import uk.gov.hmrc.http.{HttpException, HttpReads, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HttpReadsWrapperSpec extends AnyWordSpec with Matchers with MockitoSugar {

  // Mocks
  val metricRegistry: MetricRegistry = mock[MetricRegistry]
  val logger: Logger = mock[Logger]
  val response: HttpResponse = mock[HttpResponse]

  val readsSuccessUnit: Reads[Unit] = new Reads[Unit] {
    override def reads(json: JsValue): JsResult[Unit] = JsSuccess(())
  }

  val readsSuccess: Reads[String] = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = Json.fromJson[String](json)
  }

  val readsError: Reads[String] = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = Json.fromJson[String](json)
  }

  val readsErrorT: Reads[String] = new Reads[String] {
    override def reads(json: JsValue): JsResult[String] = Json.fromJson[String](json)
  }

  class TestHttpReadsWrapper(implicit readsSuccess: Reads[String],
                             readsError:   Reads[String],
                             readsErrorT:  Reads[String]) extends HttpReadsWrapper[String, String] with MetricsSupport {
    override def fromUpstreamErrorToIndividualDetailsError(
                                                            connectorName:     String,
                                                            status:            Int,
                                                            upstreamError:     String,
                                                            additionalLogInfo: Option[AdditionalLogInfo]
                                                          ): IndividualDetailsError = ConnectorError(status, "Upstream error")
    override def fromSingleUpstreamErrorToIndividualDetailsError(
                                                                  connectorName:     String,
                                                                  status:            Int,
                                                                  upstreamError:     String,
                                                                  additionalLogInfo: Option[AdditionalLogInfo]
                                                                ): Option[IndividualDetailsError] = Some(ConnectorError(status, "Single Upstream error"))
  }

  when(metricRegistry.timer(anyString())).thenReturn(new Timer())
  when(metricRegistry.counter(anyString())).thenReturn(new Counter())
  when(metricRegistry.meter(anyString())).thenReturn(new Meter())


  "HttpReadsWrapper" should {
    "handle successful response" in {
      val wrapper = new TestHttpReadsWrapper
      when(response.status).thenReturn(Status.OK)
      when(response.json).thenReturn(Json.parse("""{"test":"test"}"""))

      wrapper.withHttpReads("test", metricRegistry, None) {
        httpReads: HttpReads[Either[IndividualDetailsError, JsValue]] =>
        IndividualDetailsResponseEnvelope.fromEitherF(Future(httpReads.read("GET", "/", response)))
      } (implicitly, implicitly, implicitly) map { result =>
        result shouldEqual Right(Json.parse("""{"test":"test"}"""))
      }
    }

    "handle successful response with NO_CONTENT" in {
      val wrapper = new TestHttpReadsWrapper
      val successResponse = HttpResponse(Status.NO_CONTENT)

      when(response.status).thenReturn(successResponse.status)
      when(response.body).thenReturn("")

      wrapper.withHttpReads("test", metricRegistry) {
        httpReads: HttpReads[Either[IndividualDetailsError, Unit]] =>
          IndividualDetailsResponseEnvelope.fromEitherF(Future(httpReads.read("GET", "/", response)))
      }(
        readsSuccessUnit,
        implicitly,
        implicitly
      ) map { result =>
          result shouldEqual Right(())
      }
    }

    "handle failed response with HTTP error" in {
      val wrapper = new TestHttpReadsWrapper
      val httpException = new HttpException("HTTP Error", Status.BAD_REQUEST)
      when(response.status).thenReturn(httpException.responseCode)
      when(response.body).thenReturn("test http exception error with response code: 400")

      wrapper.withHttpReads("test", metricRegistry) {
        httpReads: HttpReads[Either[IndividualDetailsError, Unit]] =>
          IndividualDetailsResponseEnvelope.fromEitherF(Future(httpReads.read("GET", "/", response)))
      }(
        readsSuccessUnit,
        implicitly,
        implicitly
      ) map { result =>
        result shouldEqual Left(ConnectorError(httpException.responseCode, "test http exception error with response code: 400"))
      }
    }

    "handle failed response with JSON parsing error" ignore {
      val wrapper = new TestHttpReadsWrapper
      val invalidJson = "{ invalid }"
      val failedResponse = HttpResponse(Status.OK, invalidJson)

      when(response.status).thenReturn(failedResponse.status)

      wrapper.withHttpReads("test", metricRegistry) {
        httpReads: HttpReads[Either[IndividualDetailsError, String]] =>
          IndividualDetailsResponseEnvelope.fromEitherF(Future(httpReads.read("GET", "/", response)))
      }(
        implicitly,
        implicitly,
        implicitly
      ) map { result =>
        result shouldEqual Left(ConnectorError(Status.SERVICE_UNAVAILABLE, invalidJson))
      }
    }

    "handle failure response with UpstreamErrorResponse" in {
      val wrapper = new TestHttpReadsWrapper
      val upstreamErrorResponse = UpstreamErrorResponse("Upstream Error", Status.BAD_REQUEST, Status.BAD_REQUEST)

      when(response.status).thenReturn(upstreamErrorResponse.statusCode)
      when(response.body).thenReturn("test upstream error with response code: 400")

      wrapper.withHttpReads("test", metricRegistry) {
        httpReads: HttpReads[Either[IndividualDetailsError, Unit]] =>
          IndividualDetailsResponseEnvelope.fromEitherF(Future(httpReads.read("GET", "/", response)))
      }(
        readsSuccessUnit,
        implicitly,
        implicitly
      ) map { result =>
        result shouldEqual Left(ConnectorError(upstreamErrorResponse.statusCode, "test upstream error with response code: 400"))
      }
    }

    "handle Internal Server Error response" in {
      val wrapper = new TestHttpReadsWrapper
      val upstreamErrorResponse = UpstreamErrorResponse("Upstream Error", Status.INTERNAL_SERVER_ERROR, Status.INTERNAL_SERVER_ERROR)

      when(response.status).thenReturn(upstreamErrorResponse.statusCode)
      when(response.body).thenReturn("test upstream error with response code: 500")

      wrapper.withHttpReads("test", metricRegistry) {
        httpReads: HttpReads[Either[IndividualDetailsError, Unit]] =>
          IndividualDetailsResponseEnvelope.fromEitherF(Future(httpReads.read("GET", "/", response)))
      }(
        readsSuccessUnit,
        implicitly,
        implicitly
      ) map { result =>
        result shouldEqual Left(ConnectorError(upstreamErrorResponse.statusCode, "test upstream error with response code: 500"))
      }
    }
  }
}

