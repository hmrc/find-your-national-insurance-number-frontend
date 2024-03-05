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
import config.{DesApiServiceConfig, FrontendAppConfig}
import connectors.HttpReadsWrapper.Recovered
import models.errors.{ConnectorError, IndividualDetailsError}
import models.individualdetails.ResolveMerge
import models.upstreamfailure.UpstreamFailures
import org.mockito.Mockito.{mock, _}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import models.{CorrelationId, IndividualDetailsNino}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar.when
import play.api.Logger
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class DefaultIndividualDetailsConnectorSpec extends PlaySpec with MockitoSugar {

  "getIndividualDetails" should {
    "make a GET request to the correct URL" in {
      val mockHttpClient = mock[HttpClient]
      val mockAppConfig = mock[FrontendAppConfig]
      val metrics = mock[Metrics]

      val testIdentifier = IndividualDetailsNino("AB123456C")
      val testResolveMerge = ResolveMerge('Y')
      val testUrl = s"/individuals/details/NINO/${testIdentifier.value}/${testResolveMerge.value}"
      val mockDesApiServiceConfig = mock[DesApiServiceConfig]
      when(mockDesApiServiceConfig.token).thenReturn("yourToken")
      when(mockDesApiServiceConfig.environment).thenReturn("yourEnvironment")
      when(mockDesApiServiceConfig.originatorId).thenReturn("yourOriginatorId")

      when(mockAppConfig.individualDetailsServiceUrl).thenReturn("http://localhost:9000")
      when(mockAppConfig.individualDetails).thenReturn(mockDesApiServiceConfig)
      // Create a mock instance of Future[Either[IndividualDetailsError, T]]
      //val mockHttpResult = mock[Future[Either[IndividualDetailsError, String]]]

      // Define the behavior of the `recover` method
      //when(mockHttpResult.recover(any())(any())).thenReturn(Future.successful(Right("Success")))

      when(mockHttpClient.GET[HttpResponse](any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200, "Success")))

      // Create a mock instance of Recovered
      implicit val mockRecovered = mock[Recovered[String]]

      // Define the behavior of the `recovered` method
      when(mockRecovered.recovered(any[Logger], any[String], any[MetricRegistry], any[Option[AdditionalLogInfo]])(any[ExecutionContext]))
        .thenReturn(Future.successful(Right("Success")))

      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val correlationId: CorrelationId = CorrelationId(UUID.randomUUID())

      val connector = new DefaultIndividualDetailsConnector(mockHttpClient, mockAppConfig, metrics) with HttpReadsWrapper[UpstreamFailures, IndividualDetailsError] {
        override def fromUpstreamErrorToIndividualDetailsError(connectorName: String, status: Int, upstreamError: UpstreamFailures, additionalLogInfo: Option[AdditionalLogInfo]): ConnectorError =
          ???
      }


      connector.getIndividualDetails(testIdentifier, testResolveMerge)(global, hc, correlationId)
      verify(mockHttpClient).GET(eqTo(testUrl))(any(), any(), any())
    }
  }
}
