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

package services

import com.google.inject.ImplementedBy
import config.FrontendAppConfig
import connectors.NPSFMNConnector
import models.CorrelationId
import models.nps.{FailureResponse, LetterIssuedResponse, NPSFMNRequest, NPSFMNResponse, NPSFMNResponseWithFailures, NPSFMNServiceResponse, RLSDLONFAResponse, TechnicalIssueResponse}
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
    connector.sendLetter(nino.take(8), npsFMNRequest)
      .map{ response =>
        response.status match {
          case 202 => LetterIssuedResponse()
          case 400 =>
            if(checkFailure(response.body) == true) {
              val npsFMNResponse = Json.parse(response.body).as[NPSFMNResponseWithFailures]
              logger.info("************************************  service postcode response 400  ************************************ response: " + response.body)
              FailureResponse(npsFMNResponse.response.failures)
            } else {
              if (check(response.body)) {
                logger.info("************************************  service postcode response 400  ************************************ response: " + response.body)
                RLSDLONFAResponse(response.status, getMessage(response.body))
              } else
                TechnicalIssueResponse(response.status, getMessage(response.body))
            }
          case _ =>
            TechnicalIssueResponse(response.status, getMessage(response.body))
        }
    }
  }

  private def getMessage(responseBody: String): String =
    (Json.parse(responseBody) \ "origin").asOpt[String] match {
      case Some(_) => (Json.parse(responseBody) \ "response" \ "jsonServiceError" \ "message").as[String]
      case _ => (Json.parse(responseBody) \ "jsonServiceError" \ "message").as[String]
    }


  private def check(responseBody: String):Boolean = {
      val appStatusMessageList = config.npsFMNAppStatusMessageList.split(",").toList
      val npsFMNResponse = Json.parse(responseBody).as[NPSFMNResponse]
      npsFMNResponse.response.jsonServiceError.appStatusMessageList.appStatusMessage match {
        case message :: Nil => appStatusMessageList.contains(message)
        case _ => false
      }
    }
  private def checkFailure(responseBody: String):Boolean = {
    if(responseBody.contains("failures")) true else false
  }


}
