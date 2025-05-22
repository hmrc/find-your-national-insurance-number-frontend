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

import com.google.inject.ImplementedBy
import config.FrontendAppConfig
import models.CorrelationId
import models.nps.NPSFMNRequest
import play.api.Logging
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultNPSFMNConnector])
trait NPSFMNConnector {

  def sendLetter(nino: String, npsFMNRequest: NPSFMNRequest)(implicit
    hc: HeaderCarrier,
    correlationId: CorrelationId,
    ec: ExecutionContext
  ): Future[HttpResponse]
}

@Singleton
class DefaultNPSFMNConnector @Inject() (httpClientV2: HttpClientV2, appConfig: FrontendAppConfig)
    extends NPSFMNConnector
    with Logging {

  def sendLetter(nino: String, body: NPSFMNRequest)(implicit
    hc: HeaderCarrier,
    correlationId: CorrelationId,
    ec: ExecutionContext
  ): Future[HttpResponse] = {
    val url     = s"${appConfig.npsFMNAPIUrl}/nps-json-service/nps/itmp/find-my-nino/api/v1/individual/$nino"
    val headers =
      Seq("correlationId" -> correlationId.value.toString, "gov-uk-originator-id" -> appConfig.npsFMNAPIOriginatorId)

    httpClientV2
      .post(new URL(url))
      .withBody(body)
      .setHeader(headers: _*)
      .execute[HttpResponse]
      .flatMap { response =>
        Future.successful(response)
      }
  }

}
