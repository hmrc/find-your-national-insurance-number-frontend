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

package connectors
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.{DesApiServiceConfig, FrontendAppConfig}
import models.errors.{ConnectorError, InvalidIdentifier}
import models.individualdetails.*
import models.{AddressLine, CorrelationId, IndividualDetailsIdentifier, IndividualDetailsNino}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.{CONTENT_TYPE, JSON}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import util.WireMockSupport

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class IndividualDetailsConnectorSpec
    extends AnyFreeSpec
    with GuiceOneAppPerSuite
    with WireMockSupport
    with MockitoSugar {

  override def fakeApplication(): Application = {
    wireMockServer.start()
    new GuiceApplicationBuilder()
      .configure(
        "external-url.individual-details.port"     -> wiremockPort,
        "external-url.individual-details.host"     -> "127.0.0.1",
        "external-url.individual-details.protocol" -> "http"
      )
      .build()
  }

  implicit val hc: HeaderCarrier            = HeaderCarrier()
  implicit val correlationId: CorrelationId = CorrelationId.random
  implicit val ec: ExecutionContext         = app.injector.instanceOf[ExecutionContext]
  val appConfig: FrontendAppConfig          = app.injector.instanceOf[FrontendAppConfig]
  val desConfig: DesApiServiceConfig        = app.injector.instanceOf[DesApiServiceConfig]
  val httpClientV2: HttpClientV2            = app.injector.instanceOf[HttpClientV2]
  val nino: IndividualDetailsIdentifier     = IndividualDetailsNino("12345")
  val resolveMerge: ResolveMerge            = ResolveMerge('Y')
  val individualDetailsUrl                  =
    s"/find-your-national-insurance-number/individuals/details/NINO/${nino.value}/${resolveMerge.value}"

  val connector: IndividualDetailsConnector = new DefaultIndividualDetailsConnector(httpClientV2, appConfig, desConfig)

  def stubGet(url: String, responseStatus: Int, responseBody: Option[String] = None): StubMapping =
    wireMockServer.stubFor {
      val baseResponse = aResponse().withStatus(responseStatus).withHeader(CONTENT_TYPE, JSON)
      val response     = responseBody.fold(baseResponse)(body => baseResponse.withBody(body))
      get(url).willReturn(response)
    }

  val name: Name = Name(
    NameSequenceNumber(1),
    NameType.RealName,
    Some(TitleType.Mr),
    Some(RequestedName("Mister Thirty Five Characters Exact")),
    NameStartDate(LocalDate.parse("1996-12-28")),
    Some(NameEndDate(LocalDate.parse("2017-03-31"))),
    Some(OtherTitle("MR S F BRAINS")),
    Some(Honours("BSc, MA")),
    FirstForename("TESTFIRSTNAME"),
    Some(SecondForename("TESTSECONDNAME")),
    Surname("TESTSURNAME")
  )

  val address: Address = Address(
    AddressSequenceNumber(2),
    Some(AddressSource.InlandRevenue),
    CountryCode(1),
    AddressType.ResidentialAddress,
    Some(AddressStatus.NotDlo),
    LocalDate.parse("2003-04-30"),
    Some(LocalDate.parse("2009-12-31")),
    Some(LocalDate.parse("2003-04-30")),
    Some(VpaMail(254)),
    Some(DeliveryInfo("THROUGH THE LETTERBOX")),
    Some(PafReference("NO IDEA")),
    AddressLine("88 TESTING ROAD"),
    AddressLine("TESTTOWN"),
    Some(AddressLine("TESTREGION")),
    Some(AddressLine("TESTAREA")),
    Some(AddressLine("TESTSHIRE")),
    Some(AddressPostcode("XX77 6YY"))
  )

  val individualDetailsResponse: IndividualDetails = IndividualDetails(
    "AB049513",
    Some(NinoSuffix("B")),
    None,
    Some(LocalDate.parse("1978-12-17")),
    LocalDate.parse("1975-02-10"),
    Some(DateOfBirthStatus.Verified),
    Some(LocalDate.parse("2018-08-09")),
    Some(DateOfDeathStatus.NotKnown),
    Some(LocalDate.parse("1976-01-01")),
    CrnIndicator.False,
    NameList(Some(List(name))),
    AddressList(Some(List(address)))
  )

  "IndividualDetailsConnector" - {
    "make an http call to query master API to retrieve the correct response" in {
      val individualDetailsJsonResp = Json.toJson(individualDetailsResponse).toString()
      stubGet(individualDetailsUrl, OK, Some(individualDetailsJsonResp))
      val result                    = connector.getIndividualDetails(nino, resolveMerge).value.futureValue

      result mustBe a[Right[_, IndividualDetails]]
    }

    "return a ConnectorError when the API returns a non-OK status" in {
      stubGet(individualDetailsUrl, NOT_FOUND, Some("something not found"))
      val result = connector.getIndividualDetails(nino, resolveMerge).value.futureValue

      result mustBe a[Left[ConnectorError, _]]
    }

    "return a Invalid identifier error when an empty identifier is provided" in {
      val emptyNino: IndividualDetailsIdentifier = IndividualDetailsNino("")
      val result                                 = connector.getIndividualDetails(emptyNino, resolveMerge).value.futureValue

      result mustBe a[Left[InvalidIdentifier, _]]
    }
  }
}
