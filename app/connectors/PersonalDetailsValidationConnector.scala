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
import models.{CorrelationId, PDVErrorResponse, PDVNotFoundResponse, PDVResponse, PDVResponseData, PDVSuccessResponse, PDVUnexpectedResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import com.google.inject.{Inject, Singleton}
import models.pdv.PDVRequest
import play.api.Logging
import services.http.SimpleHttp
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json, OFormat, Writes}
import play.api.libs.ws.BodyWritable
import models.pdv.PDVRequest._

import java.net.URL
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonalDetailsValidationConnector @Inject()(val httpClientV2: HttpClientV2, simpleHttp: SimpleHttp, config: FrontendAppConfig) extends Logging {

  private lazy val personalDetailsValidationBaseUrl: String = config.pdvBaseUrl

  def retrieveMatchingDetails(validationId: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PDVResponse] = {
    simpleHttp.get[PDVResponse](s"$personalDetailsValidationBaseUrl/personal-details-validation/$validationId")(
      onComplete = {
        case response if response.status >= 200 && response.status < 300 =>
          PDVSuccessResponse(response.json.as[PDVResponseData])

        case response if response.status == NOT_FOUND => {
          logger.warn("Unable to find personal details record in personal-details-validation")
          PDVNotFoundResponse(response)
        }

        case response =>
          if (response.status >= INTERNAL_SERVER_ERROR)
            logger.warn(s"Unexpected ${response.status} response getting personal details record from PDV")
          PDVUnexpectedResponse(response)
      },
      onError = { e =>
        logger.warn("Error getting personal details record from personal-details-validation", e)
        PDVErrorResponse(e)
      }
    )
  }

  def retrieveMatchingDetails2(body2: PDVRequest)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    val url = s"$personalDetailsValidationBaseUrl/personal-details-validation/retrieve-by-session"
    httpClientV2
      .post(new URL(url))
      .withBody(body2)
      .execute[HttpResponse]
      .flatMap { response =>
        Future.successful(response)
      }
  }

}
