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

import config.FrontendAppConfig
import models._
import models.nps.{NPSFMNRequest}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, Injecting}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.client.HttpClientV2
import util.WireMockHelper

import java.util.UUID
import scala.util.Random

class NPSFMNConnectorSpec
  extends ConnectorSpec
    with WireMockHelper
    with MockitoSugar
    with DefaultAwaitTimeout
    with Injecting {

  override implicit lazy val app: Application = app(
    Map("external-url.nps-fmn-api.port" -> server.port(),
    )
  )

  val nino = Nino(new Generator(new Random()).nextNino.nino)

  val jsonInternalServerError = s"""
                |{
                |  "jsonServiceError": {
                |    "requestURL": "/itmp/find-my-nino/api/v1/individual/${nino.nino}",
                |    "message": "GENERIC_SERVER_ERROR",
                |    "appStatusMessageCount": 1,
                |    "appStatusMessageList": {
                |      "appStatusMessage": [
                |        "Internal Server Error"
                |      ]
                |    }
                |  }
                |}
                |""".stripMargin

  val jsonResourceNotFound =  s"""
                |{
                |  "jsonServiceError": {
                |    "requestURL": "/itmp/find-my-nino/api/v1/individual/${nino.nino}",
                |    "message": "RESOURCE_NOT_FOUND",
                |    "appStatusMessageCount": 1,
                |    "appStatusMessageList": {
                |      "appStatusMessage": [
                |        "65370"
                |      ]
                |    }
                |  }
                |}
                |""".stripMargin

  val jsonNotFound = s"""
                |{
                |  "jsonServiceError": {
                |    "requestURL": "/itmp/find-my-nino/api/v1/individual/${nino.nino}",
                |    "message": "BAD_REQUEST",
                |    "appStatusMessageCount": 1,
                |    "appStatusMessageList": {
                |      "appStatusMessage": [
                |        "63471"
                |      ]
                |    }
                |  }
                |}
                |""".stripMargin


  trait SpecSetup {

    def url(nino: String): String

    lazy val connector = {
      val httpClient2 = app.injector.instanceOf[HttpClientV2]
      val config = app.injector.instanceOf[FrontendAppConfig]
      new DefaultNPSFMNConnector(httpClient2, config)
    }
  }

  "NPS FMN Connector" must {

    trait LocalSetup extends SpecSetup {
      def url(nino: String) = s"/find-your-national-insurance-number/nps-json-service/nps/itmp/find-my-nino/api/v1/individual/${nino}"
    }

    "return Ok (200) when called with an invalid nino" in new LocalSetup {
      implicit val correlationId = CorrelationId(UUID.randomUUID())
      val body = mock[NPSFMNRequest]
      stubPost(url(nino.nino), OK, Some(Json.toJson(body).toString()), Some(""))
      val result = connector.sendLetter(nino.nino, body).futureValue.leftSideValue
      result.status mustBe OK
      result.body mustBe ""
    }

    "return NOT_FOUND (400) when called with an invalid nino" in new LocalSetup {
      implicit val correlationId = CorrelationId(UUID.randomUUID())
      val body = mock[NPSFMNRequest]
      stubPost(url(nino.nino), NOT_FOUND, Some(Json.toJson(body).toString()), Some(jsonNotFound))
      val result = connector.sendLetter(nino.nino, body).futureValue.leftSideValue
      result.status mustBe NOT_FOUND
      result.body mustBe jsonNotFound
    }

    "return RESOURCE_NOT_FOUND (400) when called with an invalid nino" in new LocalSetup {
      implicit val correlationId = CorrelationId(UUID.randomUUID())
      val body = mock[NPSFMNRequest]
      stubPost(url(nino.nino), NOT_FOUND, Some(Json.toJson(body).toString()), Some(jsonResourceNotFound))
      val result = connector.sendLetter(nino.nino, body).futureValue.leftSideValue
      result.status mustBe NOT_FOUND
      result.body mustBe jsonResourceNotFound
    }

    "return INTERNAL_SERVER_ERROR (500) when called with an invalid nino" in new LocalSetup {
      implicit val correlationId = CorrelationId(UUID.randomUUID())
      val body = mock[NPSFMNRequest]
      stubPost(url(nino.nino), INTERNAL_SERVER_ERROR, Some(Json.toJson(body).toString()), Some(jsonInternalServerError))
      val result = connector.sendLetter(nino.nino, body).futureValue.leftSideValue
      result.status mustBe INTERNAL_SERVER_ERROR
      result.body mustBe jsonInternalServerError
    }
  }

}