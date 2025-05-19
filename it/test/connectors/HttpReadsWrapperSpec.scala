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

package connectors

import com.codahale.metrics.{Counter, Meter, MetricRegistry, Timer}
import models.IndividualDetailsResponseEnvelope
import models.errors.{ConnectorError, IndividualDetailsError}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpException, HttpReads, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HttpReadsWrapperSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures
    with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(metricRegistry, response, logger)
  }

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(1, Seconds))

  // Mocks
  val metricRegistry: MetricRegistry = mock[MetricRegistry]
  val logger: Logger                 = mock[Logger]
  val response: HttpResponse         = mock[HttpResponse]

  val readsSuccessUnit: Reads[Unit] = (json: JsValue) => JsSuccess(())

  val readsSuccess: Reads[String] = (json: JsValue) => Json.fromJson[String](json)

  val readsError: Reads[String] = (json: JsValue) => Json.fromJson[String](json)

  val readsErrorT: Reads[String] = (json: JsValue) => Json.fromJson[String](json)

  class TestHttpReadsWrapper extends HttpReadsWrapper[String, String] {
    override def fromUpstreamErrorToIndividualDetailsError(
      connectorName: String,
      status: Int,
      upstreamError: String,
      additionalLogInfo: Option[AdditionalLogInfo]
    ): IndividualDetailsError = ConnectorError(status, "Upstream error")

    override def fromSingleUpstreamErrorToIndividualDetailsError(
      connectorName: String,
      status: Int,
      upstreamError: String,
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
      }(implicitly, implicitly, implicitly) map { result =>
        result shouldEqual Right(Json.parse("""{"test":"test"}"""))
      }
    }

    "handle failed response with HTTP error" in {
      val wrapper       = new TestHttpReadsWrapper
      val httpException = new HttpException("HTTP Error", Status.BAD_REQUEST)
      when(response.status).thenReturn(httpException.responseCode)
      when(response.body).thenReturn("""{"test upstream error with response code:": 400}""")

      wrapper.withHttpReads("test", metricRegistry) { httpReads: HttpReads[Either[IndividualDetailsError, Unit]] =>
        IndividualDetailsResponseEnvelope.fromEitherF(Future(httpReads.read("GET", "/", response)))
      }(
        readsSuccessUnit,
        implicitly,
        implicitly
      ) map { result =>
        result shouldEqual Left(
          ConnectorError(httpException.responseCode, "test http exception error with response code: 400")
        )
      }
    }

    "handle failed response with JSON parsing error" in {
      val wrapper        = new TestHttpReadsWrapper
      val invalidJson    = "{ invalid }"
      val failedResponse = HttpResponse(Status.OK, invalidJson)

      when(response.status).thenReturn(failedResponse.status)

      wrapper.withHttpReads("test", metricRegistry) { httpReads: HttpReads[Either[IndividualDetailsError, String]] =>
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
      val wrapper               = new TestHttpReadsWrapper
      val upstreamErrorResponse = UpstreamErrorResponse("Upstream Error", Status.BAD_REQUEST, Status.BAD_REQUEST)

      when(response.status).thenReturn(upstreamErrorResponse.statusCode)
      when(response.body).thenReturn("""{"test upstream error with response code:": 400}""")

      wrapper.withHttpReads("test", metricRegistry) { httpReads: HttpReads[Either[IndividualDetailsError, Unit]] =>
        IndividualDetailsResponseEnvelope.fromEitherF(Future(httpReads.read("GET", "/", response)))
      }(
        readsSuccessUnit,
        implicitly,
        implicitly
      ) map { result =>
        result shouldEqual Left(
          ConnectorError(upstreamErrorResponse.statusCode, "test upstream error with response code: 400")
        )
      }
    }

    "handle Internal Server Error response" in {
      val wrapper               = new TestHttpReadsWrapper
      val upstreamErrorResponse =
        UpstreamErrorResponse("Upstream Error", Status.INTERNAL_SERVER_ERROR, Status.INTERNAL_SERVER_ERROR)

      when(response.status).thenReturn(upstreamErrorResponse.statusCode)
      when(response.body).thenReturn("""{"test upstream error with response code:": 500}""")

      wrapper.withHttpReads("test", metricRegistry) { httpReads: HttpReads[Either[IndividualDetailsError, Unit]] =>
        IndividualDetailsResponseEnvelope.fromEitherF(Future(httpReads.read("GET", "/", response)))
      }(
        readsSuccessUnit,
        implicitly,
        implicitly
      ) map { result =>
        result shouldEqual Left(
          ConnectorError(upstreamErrorResponse.statusCode, "test upstream error with response code: 500")
        )
      }
    }
  }
}
