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
import models.{PDVNotFoundResponse, PDVNotFoundResponse$, PDVResponse, PDVResponseData, PDVSuccessResponse, PersonalDetails}
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.libs.json.Json
import play.api.test.{DefaultAwaitTimeout, Injecting}
import services.http.SimpleHttp
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HttpResponse
import util.WireMockHelper

import java.time.LocalDate
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

  val validationId: String = "ab1lp2345jgoauskg5345"

  trait SpecSetup {
    def url: String
    val fakeNino = Nino(new Generator(new Random()).nextNino.nino)

    val personalDetails: PersonalDetails =
      PersonalDetails(
        "firstName",
        "lastName",
        fakeNino,
        Some("AA1 1AA"),
        LocalDate.parse("1945-03-18")
      )
    val personalDetailsValidation: PDVResponseData =
      PDVResponseData(
        validationId,
        "success",
        Some(personalDetails)
      )

    lazy val connector = {
      val httpClient = app.injector.instanceOf[SimpleHttp]
      val config = app.injector.instanceOf[FrontendAppConfig]
      new PersonalDetailsValidationConnector(httpClient, config)
    }
  }

  "Calling retrieveMatchingDetails" must {
    trait LocalSetup extends SpecSetup {
      def url: String = s"/personal-details-validation/$validationId"
    }

    "return OK when called with an existing validationId" in new LocalSetup {
      stubGet(url, OK, Some(Json.toJson(personalDetailsValidation).toString()))
      val result = connector.retrieveMatchingDetails(validationId).futureValue.leftSideValue
      result.asInstanceOf[PDVSuccessResponse].pdvResponseData mustBe personalDetailsValidation
    }

    "return NOT_FOUND when called with an unknown validationId" in new LocalSetup {
      stubGet(url, NOT_FOUND, None)
      val result = connector.retrieveMatchingDetails(validationId).futureValue.leftSideValue
      result.asInstanceOf[PDVNotFoundResponse].r.status mustBe NOT_FOUND
    }
  }

}
