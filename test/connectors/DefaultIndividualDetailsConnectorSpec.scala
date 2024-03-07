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
import com.codahale.metrics.MetricRegistry
import com.typesafe.config.ConfigFactory
import config.{DesApiServiceConfig, FrontendAppConfig}
import models.errors.{ConnectorError, IndividualDetailsError}
import models.individualdetails._
import models.{AddressLine, CorrelationId, IndividualDetailsNino}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class IndividualDetailsConnectorSpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IndividualDetailsConnectorFixture {

  "IndividualDetailsConnector" should {
    "make an http call to query master API to retrieve the correct response" in {
      val connector = new DefaultIndividualDetailsConnector(httpClient, appConfig, metrics)

      (httpClient.GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(
            _: HttpReads[Either[IndividualDetailsError, IndividualDetails]],
            _: HeaderCarrier, _: ExecutionContext)).expects(
          s"$individualDetailsUrl${nino.value}/${resolveMerge.value}",
          *, *, *, *, *)
        .returning(Future successful Right(individualDetailsResponse))
        .once()

      whenReady(connector.getIndividualDetails(IndividualDetailsNino(nino.value), resolveMerge).value) { r =>
        r mustBe Right(individualDetailsResponse)
      }
    }
    "return a ConnectorError " in {
      val connector = new DefaultIndividualDetailsConnector(httpClient, appConfig, metrics)

      (httpClient.GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])
          (_: HttpReads[Either[IndividualDetailsError, IndividualDetails]],
            _: HeaderCarrier, _: ExecutionContext)).expects(
          s"$individualDetailsUrl${nino.value}/${resolveMerge.value}",
          *, *, *, *, *)
        .returning(Future successful Left(ConnectorError(NOT_FOUND, "something not found")))
        .once()

      whenReady(connector.getIndividualDetails(IndividualDetailsNino(nino.value), resolveMerge).value) { r =>
        r mustBe Left(ConnectorError(NOT_FOUND, "something not found"))
      }
    }
  }
}
trait IndividualDetailsConnectorFixture extends MockFactory {
  implicit val hc:            HeaderCarrier = HeaderCarrier()
  implicit val correlationId: CorrelationId = CorrelationId.random

  val metrics = new Metrics {
    override def defaultRegistry: MetricRegistry = new MetricRegistry()
  }

  private val config = ConfigFactory.load(); // read Config here
  private val myconfig = Configuration(config)
  private val myServicesConfig = new ServicesConfig(myconfig)

  val nino                 = IndividualDetailsNino("12345")
  val resolveMerge         = ResolveMerge('Y')
  val individualDetailsUrl = "http://localhost:14022/find-your-national-insurance-number/individuals/details/NINO/"
  val individualDetailsConfig = DesApiServiceConfig("token", "env", "corr-id")
  val httpClient: HttpClient       = mock[HttpClient]
  val appConfig:  FrontendAppConfig  = new FrontendAppConfig(myconfig, myServicesConfig)
  val ec:         ExecutionContext = implicitly[ExecutionContext]


  val name = Name(
    NameSequenceNumber(1),
    NameType.RealName,
    Some(TitleType.Mr),
    Some(RequestedName("Mister Thirty Five Characters Exact")),
    NameStartDate(LocalDate.parse("1996-12-28")),
    Some(NameEndDate(LocalDate.parse("2017-03-31"))),
    Some(OtherTitle("MR S F BRAINS")),
    Some(Honours("BSc, MA, NUMPTY")),
    FirstForename("TESTFIRSTNAME"),
    Some(SecondForename("TESTSECONDNAME")),
    Surname("TESTSURNAME")
  )

  val address = Address(
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

  val individualDetailsResponse = IndividualDetails(
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
}