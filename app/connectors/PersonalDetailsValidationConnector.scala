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

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import models.pdv.PDVRequest
import models.pdv.PDVRequest._
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonalDetailsValidationConnector @Inject()(val httpClientV2: HttpClientV2, config: FrontendAppConfig) extends Logging {

  def retrieveMatchingDetails(pdvRequest: PDVRequest)
                             (implicit hc: HeaderCarrier, ex: ExecutionContext): Future[HttpResponse] = {
    val url = s"${config.pdvBaseUrl}/personal-details-validation/retrieve-by-session"

    httpClientV2
      .post(new URL(url))
      .withBody(pdvRequest)
      .execute[HttpResponse]
  }

}
