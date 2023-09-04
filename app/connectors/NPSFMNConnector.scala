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
import com.kenshoo.play.metrics.Metrics
import config.FrontendAppConfig
import models.errors.{ConnectorError, IndividualDetailsError}
import models.nps.{NPSFMNRequest, NPSFMNResponse}
import models.upstreamfailure.{Failure, UpstreamFailures}
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, CONFLICT, INTERNAL_SERVER_ERROR, METHOD_NOT_ALLOWED,
  NOT_FOUND, NOT_IMPLEMENTED, UNAUTHORIZED, UNSUPPORTED_MEDIA_TYPE}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

//case class NPSFMNResponse()
//final case object LetterIssuedResponse extends NPSFMNResponse
//final case object RLSDLONFAResponse extends NPSFMNResponse
//final case object TechnicalIssueResponse extends NPSFMNResponse

@ImplementedBy(classOf[DefaultNPSFMNConnector])
trait NPSFMNConnector {

  def updateDetails(nino: String, npsFMNRequest: NPSFMNRequest
                   )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue]

}

@Singleton
class DefaultNPSFMNConnector@Inject() (httpClient: HttpClient,
  appConfig:  FrontendAppConfig, metrics: Metrics) extends  NPSFMNConnector
  with HttpReadsWrapper[UpstreamFailures, Failure]
  with MetricsSupport {

  def updateDetails(nino: String, npsFMNRequest: NPSFMNRequest
                   )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue] = {
    val url = s"${appConfig.individualDetailsServiceUrl}/nps-json-service/nps/itmp/find-my-nino/api/v1/individual/$nino"

    println(s"\n\n\n  url = $url  \n\n\n")

    val r = httpClient.POST[JsValue, HttpResponse](url, Json.toJson(npsFMNRequest))(implicitly, implicitly, hc, implicitly)

    println(s"\n\n\n  r = ${r}  \n\n\n")

      r.map { response =>

        println(s"\n\n\n  response = $response  \n\n\n")

        response.status match {
          case ACCEPTED => //LetterIssuedResponse
            Json.toJson(response.body)
//          case BAD_REQUEST | UNAUTHORIZED | NOT_FOUND | METHOD_NOT_ALLOWED
//               | CONFLICT | UNSUPPORTED_MEDIA_TYPE |
//               INTERNAL_SERVER_ERROR | NOT_IMPLEMENTED =>
//            Json.toJson(response.body)
          case _ => throw new HttpException(response.body, response.status)
        }
      }.recover {
        case e: Exception =>

          import scala.util.{Success, Failure}

          println(s"\n\n\n ################ From recover: response = ${e.getMessage}  \n\n\n")
          r.value match {
            case Some(t) =>
              t match {
                case Failure(f) =>
                  if(f.toString.contains("63471") | f.toString.contains("63472") | f.toString.contains("63473")) {
                    println("RLSDLONFAResponse")
                    //RLSDLONFAResponse
                  }
                  Json.toJson("{}")
                case Success(s)=>
                  println(s"\n\n\n  Success response body = ${s.body}  status = ${s.status} \n\n\n")
                  Json.toJson("{}")
              }

            case None => throw new HttpException("......... error ........", 500)
          }
          Json.toJson("{}")
        case _ =>
          Json.toJson("{}")
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