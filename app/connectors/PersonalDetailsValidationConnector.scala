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
import models.{PersonalDetailsValidation, PersonalDetailsValidationErrorResponse, PersonalDetailsValidationNotFoundResponse, PersonalDetailsValidationResponse, PersonalDetailsValidationSuccessResponse, PersonalDetailsValidationUnexpectedResponse}
import uk.gov.hmrc.http.HeaderCarrier
import com.google.inject.{Inject, Singleton}
import play.api.Logging
import services.http.SimpleHttp
import play.api.http.Status._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonalDetailsValidationConnector @Inject()(val simpleHttp: SimpleHttp, config: FrontendAppConfig) extends Logging {

  private lazy val personalDetailsValidationBaseUrl: String = config.pdvBaseUrl

  def retrieveMatchingDetails(validationId: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[PersonalDetailsValidationResponse] = {
    simpleHttp.get[PersonalDetailsValidationResponse](s"$personalDetailsValidationBaseUrl/personal-details-validation/$validationId")(
      onComplete = {
        case response if response.status >= 200 && response.status < 300 =>
          PersonalDetailsValidationSuccessResponse(response.json.as[PersonalDetailsValidation])

        case response if response.status == NOT_FOUND =>
          logger.warn("Unable to find personal details record in personal-details-validation")
          PersonalDetailsValidationNotFoundResponse

        case response =>
          if (response.status >= INTERNAL_SERVER_ERROR) {
            logger.warn(
              s"Unexpected ${response.status} response getting personal details record from personal-details-validation"
            )
          }
          PersonalDetailsValidationUnexpectedResponse(response)
      },
      onError = { e =>
        logger.warn("Error getting personal details record from personal-details-validation", e)
        PersonalDetailsValidationErrorResponse(e)
      }
    )
  }

}
