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

import config.FrontendAppConfig
import models.pdv.{PDVRequest, PDVResponseData, PersonalDetails}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, Injecting}
import play.api.mvc.Result
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{BadRequestException, HttpResponse, NotFoundException}
import uk.gov.hmrc.http.client.HttpClientV2
import util.WireMockHelper
import play.api.mvc.Results.Ok
import play.api.test.Helpers.await

import java.time.{LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import scala.util.Random

class PDVResponseDataConnectorSpec
  extends ConnectorSpec
    with WireMockHelper
    with MockitoSugar
    with DefaultAwaitTimeout
    with Injecting {

  override implicit lazy val app: Application = app(
    Map("microservice.services.personal-details-validation.port" -> server.port())
  )

  val headers: Seq[(String, String)] = Seq(
    "CorrelationId" -> "1118057e-fbbc-47a8-a8b4-78d9f015c253",
    "Content-Type" -> "application/json"
  )

  val headers2: Map[String,Seq[String]] = Map(
    "CorrelationId" -> Seq("1118057e-fbbc-47a8-a8b4-78d9f015c253"),
    "Content-Type" -> Seq("application/json")
  )

  val id =  "10123456789"

  def PDV200SuccessResponseforCRNFailure: Result = Ok(
    s"""
       |{
       |  "id": $id,
       |  "validationStatus": "success",
       |  "personalDetails": {
       |    "firstName": "Jim",
       |    "lastName": "Ferguson",
       |    "nino": "AA000004B",
       |    "dateOfBirth": "1948-04-23",
       |    "postCode" : "AA1 1AA"
       |  }
       |}
       |""".stripMargin)
    .as("application/json")
    .withHeaders(headers: _*)

  val body =
    s"""
       |{
       |  "id": $id,
       |  "validationStatus": "success",
       |  "personalDetails": {
       |    "firstName": "Jim",
       |    "lastName": "Ferguson",
       |    "nino": "AA000004B",
       |    "dateOfBirth": "1948-04-23",
       |    "postCode" : "AA1 1AA"
       |  }
       |}
       |""".stripMargin

  val httpResponse = HttpResponse(200, body, headers2)

  trait SpecSetup {
    def url: String
    val fakeNino = Nino(new Generator(new Random()).nextNino.nino)

    val personalDetails: PersonalDetails =
      PersonalDetails(
        "Jim",
        "Ferguson",
        Nino("AA000004B"),
        Some("AA1 1AA"),
        LocalDate.parse("1945-03-18")
      )
    val personalDetailsValidation: PDVResponseData =
      PDVResponseData(
        id,
        "success",
        Some(personalDetails),
        lastUpdated = LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC),
        reason = None,
        validCustomer = None,
        CRN = None,
        npsPostCode = None
      )

    lazy val connector: PersonalDetailsValidationConnector = {
      val httpClient2 = app.injector.instanceOf[HttpClientV2]
      val config = app.injector.instanceOf[FrontendAppConfig]
      new PersonalDetailsValidationConnector(httpClient2, config)
    }
  }

  "Calling retrieveMatchingDetails" must {
    trait LocalSetup extends SpecSetup {
      def url: String = s"/personal-details-validation/retrieve-by-session"
    }

    "return OK when called with an existing validationId" in new LocalSetup {
      val pdvRequest: PDVRequest = PDVRequest("pdv-success-not-crn", "dummy")
      stubPost(url, OK, Some(Json.toJson(pdvRequest).toString()), Some(Json.toJson(personalDetailsValidation).toString()))
      val result: HttpResponse = connector.retrieveMatchingDetails(pdvRequest).futureValue.leftSideValue
      result.status mustBe OK
      Json.parse(result.body).as[PDVResponseData].personalDetails mustBe personalDetailsValidation.personalDetails
    }

    "return NOT_FOUND when called with an unknown validationId" in new LocalSetup {

      val body =
        s"""
           |{
           |  "error": "No association found"
           |}
           |""".stripMargin

      val pdvRequest: PDVRequest = PDVRequest("not found", "dummy")
      stubPost(url, NOT_FOUND, Some(Json.toJson(pdvRequest).toString()), Some(body))

      intercept[NotFoundException] {
        await(connector.retrieveMatchingDetails(pdvRequest))
      }
    }

    "return BAD_REQUEST when called with invalid data" in new LocalSetup {

      val pdvRequest: PDVRequest = PDVRequest("invalid", "dummy")
      stubPost(url, BAD_REQUEST, Some(Json.toJson(pdvRequest).toString()), None)

      intercept[BadRequestException] {
        await(connector.retrieveMatchingDetails(pdvRequest))
      }
    }
  }

}
