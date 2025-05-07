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

package models.nps

import play.api.libs.json.{Format, Json}

case class AppStatusMessageList(appStatusMessage: List[String] = List.empty)
case class JsonServiceError(
  requestURL: String,
  message: String,
  appStatusMessageCount: Int,
  appStatusMessageList: AppStatusMessageList
)
case class Response(jsonServiceError: JsonServiceError)

case class NPSFMNResponse(origin: String, response: Response)

case class Failure(`type`: String, reason: String)
object Failure {
  implicit val format: Format[Failure] = Json.format[Failure]
}

case class ResponseWithFailures(failures: List[Failure])
object ResponseWithFailures {
  implicit val format: Format[ResponseWithFailures] = Json.format[ResponseWithFailures]
}

case class NPSFMNResponseWithFailures(origin: String, response: ResponseWithFailures)
object NPSFMNResponseWithFailures {
  implicit val format: Format[NPSFMNResponseWithFailures] = Json.format[NPSFMNResponseWithFailures]
}

object NPSFMNResponse {
  implicit val appStatusMessageListformat: Format[AppStatusMessageList] = Json.format[AppStatusMessageList]
  implicit val jsonServiceErrorformat: Format[JsonServiceError]         = Json.format[JsonServiceError]
  implicit val jsonResponse: Format[Response]                           = Json.format[Response]
  implicit val npsFMNResponseformat: Format[NPSFMNResponse]             = Json.format[NPSFMNResponse]
  implicit val failureFormat: Format[Failure]                           = Json.format[Failure]
}

sealed trait NPSFMNServiceResponse
final case class LetterIssuedResponse() extends NPSFMNServiceResponse
final case class RLSDLONFAResponse(status: Int, message: String) extends NPSFMNServiceResponse
final case class TechnicalIssueResponse(status: Int, message: String) extends NPSFMNServiceResponse
final case class FailureResponse(failures: List[Failure]) extends NPSFMNServiceResponse
