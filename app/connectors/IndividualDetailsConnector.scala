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

import cats.data.EitherT
import cats.syntax.all._
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import config.FrontendAppConfig
import connectors.HttpReadsWrapper.Recovered
import models.IndividualDetailsResponseEnvelope.IndividualDetailsResponseEnvelope
import models.errors.{IndividualDetailsError, ConnectorError}
import models.{IndividualDetailsIdentifier, CorrelationId}
import models.individualdetails.{IndividualDetails, ResolveMerge}
import models.upstreamfailure.{Failure, UpstreamFailures}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[DefaultIndividualDetailsConnector])
trait IndividualDetailsConnector {
  def getIndividualDetails(identifier: IndividualDetailsIdentifier, resolveMerge: ResolveMerge
                          )(implicit ec: ExecutionContext,
                            hc: HeaderCarrier,
                            correlationId: CorrelationId): IndividualDetailsResponseEnvelope[IndividualDetails]
}

@Singleton
class DefaultIndividualDetailsConnector @Inject() (httpClient: HttpClient,
    appConfig:  FrontendAppConfig, metrics: Metrics) extends IndividualDetailsConnector
    with HttpReadsWrapper[UpstreamFailures, Failure]
    with MetricsSupport {

  def getIndividualDetails(identifier: IndividualDetailsIdentifier, resolveMerge: ResolveMerge
                          )(implicit ec: ExecutionContext,hc: HeaderCarrier, correlationId: CorrelationId
  ): IndividualDetailsResponseEnvelope[IndividualDetails] = {
    val url = s"${appConfig.individualDetailsServiceUrl}/individuals/details/NINO/${identifier.value}/${resolveMerge.value}"
    val connectorName     = "individual-details-connector"
    val additionalLogInfo = Some(AdditionalLogInfo(Map("correlation-id" -> correlationId.value.toString)))
    withHttpReads(
      connectorName,
      metrics.defaultRegistry,
      additionalLogInfo
    ) { implicit httpReads =>
      EitherT(
        httpClient
          .GET(url)(httpReads, desApiHeaders(appConfig.individualDetails), ec)
          .recovered(logger, connectorName, metrics.defaultRegistry, additionalLogInfo)
      )
    }
  }

  override def fromUpstreamErrorToIndividualDetailsError(
      connectorName:     String,
      status:            Int,
      upstreamError:     UpstreamFailures,
      additionalLogInfo: Option[AdditionalLogInfo]
  ): ConnectorError = {
    val additionalLogInformation = additionalLogInfo.map(ali => s"${ali.toString}, ").getOrElse("")
    logger.debug(s"$additionalLogInformation$connectorName with status: $status, ${upstreamError.failures
      .map(f => s"code: ${f.code}. reason: ${f.reason}")
      .mkString(";")}")

    ConnectorError(
      status,
      s"$connectorName, ${upstreamError.failures.map(f => s"code: ${f.code}. reason: ${f.reason}").mkString(";")}"
    )
  }

  override def fromSingleUpstreamErrorToIndividualDetailsError(
      connectorName:     String,
      status:            Int,
      upstreamError:     Failure,
      additionalLogInfo: Option[AdditionalLogInfo]
  ): Option[IndividualDetailsError] = {
    val additionalLogInformation = additionalLogInfo.map(ali => s"${ali.toString}, ").getOrElse("")

    logger.debug(
      s"$additionalLogInformation$connectorName with status: $status, ${upstreamError.code} - ${upstreamError.reason}"
    )
    ConnectorError(status, s"$connectorName, ${upstreamError.code} - ${upstreamError.reason}").some
  }

}
