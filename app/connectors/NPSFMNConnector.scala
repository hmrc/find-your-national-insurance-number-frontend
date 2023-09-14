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

import cats.syntax.all._
import com.google.inject.ImplementedBy
import config.FrontendAppConfig
import models.CorrelationId
import models.errors.{ConnectorError, IndividualDetailsError}
import models.nps.NPSFMNRequest
import models.upstreamfailure.{Failure, UpstreamFailures}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultNPSFMNConnector])
trait NPSFMNConnector {

  def updateDetails(nino: String, npsFMNRequest: NPSFMNRequest
                   )(implicit hc: HeaderCarrier,correlationId: CorrelationId, ec: ExecutionContext): Future[HttpResponse]
}

@Singleton
class DefaultNPSFMNConnector@Inject() (httpClientV2: HttpClientV2, appConfig: FrontendAppConfig)
  extends  NPSFMNConnector
  with HttpReadsWrapper[UpstreamFailures, Failure]
  with MetricsSupport {

  def updateDetails(nino: String, body: NPSFMNRequest
                   )(implicit hc: HeaderCarrier,correlationId: CorrelationId, ec: ExecutionContext): Future[HttpResponse] = {
    val url = s"${appConfig.npsFMNAPIUrl}/nps-json-service/nps/itmp/find-my-nino/api/v1/individual/$nino"
    val headers = Seq("correlationId" -> correlationId.value.toString,
      "gov-uk-originator-id" -> "FIND_MY_NINO")

    httpClientV2
      .post(new URL(url))
      .withBody(body)
      .setHeader(headers:_*)
      .execute[HttpResponse]
      .flatMap{ response =>
        Future.successful(response)
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