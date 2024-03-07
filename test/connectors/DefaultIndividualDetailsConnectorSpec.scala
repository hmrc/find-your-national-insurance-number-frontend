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
import models.individualdetails.{IndividualDetails, ResolveMerge}
import models.{CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito._
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Logger
import play.api.http.Status.INTERNAL_SERVER_ERROR
import services.CheckDetailsServiceSpec.fakeIndividualDetails
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import uk.gov.hmrc.play.bootstrap.metrics._

import java.net.URL
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DefaultIndividualDetailsConnectorSpec extends PlaySpec with MockitoSugar {

  "getIndividualDetails" should {
    "make a GET request and return correct results" in {

      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val correlationId: CorrelationId = CorrelationId(UUID.randomUUID())

      val testIdentifier = IndividualDetailsNino("AB123456C")
      val testResolveMerge = ResolveMerge('Y')
      val testUrl = s"http://localhost:9000/individuals/details/NINO/${testIdentifier.value}/${testResolveMerge.value}"

      val mockHttpClient = mock[HttpClient]
      val mockAppConfig = mock[FrontendAppConfig]
      val metrics = mock[Metrics]
      val mockAdditionalLogInfo = AdditionalLogInfo(Map("correlation-id" -> "1234"))
      val mockLogger = mock[Logger]
      val mockConnectorName = "individual-details-connector"
      val mockMetrics = mock[uk.gov.hmrc.play.bootstrap.metrics.Metrics]
      val mockDesApiServiceConfig = mock[DesApiServiceConfig]
      val mockHttpReads = mock[HttpReads[Either[IndividualDetailsError, IndividualDetails]]]

      when(mockDesApiServiceConfig.token).thenReturn("yourToken")
      when(mockDesApiServiceConfig.environment).thenReturn("yourEnvironment")
      when(mockDesApiServiceConfig.originatorId).thenReturn("yourOriginatorId")

      when(mockAppConfig.individualDetailsServiceUrl).thenReturn("http://localhost:9000")
      when(mockAppConfig.individualDetails).thenReturn(mockDesApiServiceConfig)
      when(mockMetrics.defaultRegistry).thenReturn(new MetricRegistry())


      //      when(mockHttpReads.read(any(), any(), any())).thenReturn(
      //        (Left(ConnectorError(INTERNAL_SERVER_ERROR, s"downstream error with status code"))))

      //            when(mockHttpReads.read(any(), any(), any())).thenReturn(
      //              (Right(fakeIndividualDetails)))

      when(mockHttpClient.GET[Either[IndividualDetailsError, IndividualDetails]](new URL(testUrl))(mockHttpReads, hc, global))
        .thenReturn(
          Future.successful(Right(fakeIndividualDetails))
        )

      when(mockHttpClient.GET[Either[IndividualDetailsError, IndividualDetails]](new URL(testUrl))(mockHttpReads, hc, global)
        .recovered(mockLogger, mockConnectorName, mockMetrics.defaultRegistry, Some(mockAdditionalLogInfo))(global))
          .thenReturn(
            Future.successful(Left(ConnectorError(INTERNAL_SERVER_ERROR, s"downstream error with status code")))
          )


      val connector = new DefaultIndividualDetailsConnector(mockHttpClient, mockAppConfig, metrics)

      val res = connector.getIndividualDetails(testIdentifier, testResolveMerge)
      res mustBe IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails))

      //verify(mockHttpClient).GET[Either[IndividualDetailsError, IndividualDetails]](new URL(testUrl))(mockHttpReads, hc, global)
      //
    }
  }
}
