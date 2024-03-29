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
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}
import com.google.inject.{Inject, Singleton}
import models.pdv.PDVRequest
import play.api.Logging
import uk.gov.hmrc.http.client.HttpClientV2
import models.pdv.PDVRequest._
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import services.AuditService

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonalDetailsValidationConnector @Inject()(val httpClientV2: HttpClientV2, config: FrontendAppConfig, auditService: AuditService) extends Logging {

  def retrieveMatchingDetails(pdvRequest: PDVRequest)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    val url = s"${config.pdvBaseUrl}/personal-details-validation/retrieve-by-session"
      httpClientV2
      .post(new URL(url))
      .withBody(pdvRequest)
      .execute[HttpResponse]
      .flatMap { response =>
        Future.successful(response)
      } recover {
        case e: HttpException if e.responseCode == NOT_FOUND || e.responseCode == BAD_REQUEST =>
          auditService.findYourNinoGetPdvDataHttpError(e.responseCode.toString, e.message)
          HttpResponse(e.responseCode, e.message)
        case _ =>
          auditService.findYourNinoGetPdvDataHttpError(INTERNAL_SERVER_ERROR.toString, "Service unavailable")
          HttpResponse(INTERNAL_SERVER_ERROR, "Service unavailable")
      }
  }

}
