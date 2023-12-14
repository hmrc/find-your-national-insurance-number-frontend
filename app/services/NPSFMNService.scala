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

package services

import com.google.inject.ImplementedBy
import config.FrontendAppConfig
import connectors.NPSFMNConnector
import models.CorrelationId
import models.nps.{LetterIssuedResponse, NPSFMNRequest, NPSFMNResponse, NPSFMNServiceResponse, RLSDLONFAResponse, TechnicalIssueResponse}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[NPSFMNServiceImpl])
trait NPSFMNService {
  def sendLetter(nino: String, npsFMNRequest: NPSFMNRequest
                   )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NPSFMNServiceResponse]
}

class NPSFMNServiceImpl @Inject()(connector: NPSFMNConnector,
  config: FrontendAppConfig)(implicit val ec: ExecutionContext)
  extends NPSFMNService with Logging {

  def sendLetter(nino: String, npsFMNRequest: NPSFMNRequest
                   )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NPSFMNServiceResponse] = {
    implicit val correlationId: CorrelationId = CorrelationId(UUID.randomUUID())
    val identifier = nino.substring(0, nino.length-1)
    connector.updateDetails(identifier, npsFMNRequest)
      .map{ response =>
        response.status match {
          case 202 =>
            LetterIssuedResponse()
          case 400 if check(response.body) =>
            RLSDLONFAResponse(response.status, (Json.parse(response.body) \ "jsonServiceError" \ "message").as[String])
          case _ =>
            TechnicalIssueResponse(response.status, (Json.parse(response.body) \ "jsonServiceError" \ "message").as[String])
        }
    }
  }

  private def check(responseBody: String):Boolean = {
    val appStatusMessageList = config.npsFMNAppStatusMessageList.split(",").toList
    val response = Json.parse(responseBody).as[NPSFMNResponse]
    response.jsonServiceError.appStatusMessageList.appStatusMessage match {
      case message :: Nil => appStatusMessageList.contains(message)
      case _ => false
    }
  }

}
