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

package util

import models.pdv.PersonalDetails
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

object AuditUtils {

  val auditSource = "find-your-national-insurance-number-frontend"

  case class YourDetailsAuditEvent(
                                    postcodeChecked: Option[String],
                                    ninoChecked: Option[String],
                                    personalDetailsValidationOutcome: String,
                                    personalDetailsValidationIdentifierType: String,
                                    identifierFromPersonalDetailsValidation: String,
                                    findMyNinoOption: Option[String],
                                    findMyNinoPostcodeEntered: Option[String],
                                    findMyNinoPostcodeMatched: Option[String],
                                    pageErrorGeneratedFrom: Option[String],
                                    errorStatus: Option[String],
                                    errorReason: Option[String]
                                  )

  object YourDetailsAuditEvent {
    implicit val format: OFormat[YourDetailsAuditEvent] = Json.format[YourDetailsAuditEvent]
  }

  private def buildDataEvent(auditType: String, transactionName: String, detail: JsValue)(implicit
                                                                                          hc: HeaderCarrier
  ): ExtendedDataEvent = {
    val strPath = hc.otherHeaders.toMap.get("path")
    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      tags = Map(
        "transactionName" -> Some(transactionName),
        "X-Session-ID" -> hc.sessionId.map(_.value),
        "X-Request-ID" -> hc.requestId.map(_.value),
        "clientIP" -> hc.trueClientIp,
        "clientPort" -> hc.trueClientPort,
        "deviceID" -> hc.deviceID,
        "path" -> strPath
      ).map(x => x._2.map((x._1, _))).flatten.toMap,
      detail = detail
    )
  }

  private def buildDetails(postcode: Option[String], nino: Option[String], validationOutcome: String, identifierType: String,
                           pdvId: String, findMyNinoOption: Option[String], findMyNinoPostcodeEntered: Option[String],
                           findMyNinoPostcodeMatched: Option[String], pageErrorGeneratedFrom: Option[String],
                           errorStatus: Option[String], errorReason: Option[String]): YourDetailsAuditEvent = {

    YourDetailsAuditEvent(
      postcode,
      nino,
      validationOutcome,
      identifierType,
      pdvId,
      findMyNinoOption,
      findMyNinoPostcodeEntered,
      findMyNinoPostcodeMatched,
      pageErrorGeneratedFrom,
      errorStatus,
      errorReason
    )
  }

  private def checkCRN(crnIndicator: String): String = {
    crnIndicator match {
      case "true" => "CRN"
      case "false" => "Nino"
      case _ => ""
    }
  }

  private def getValidationOutcome(validationOutcome: String): String = {
    validationOutcome match {
      case "success" => "Matched"
      case "failure" => "Unmatched"
      case _ => ""
    }
  }

  private def getFindMyNinoOption(findMyNinoOption: Option[String]): Option[String] = {
    findMyNinoOption match {
      case Some("true") => Some("Send letter")
      case Some("false") => Some("No to send letter")
      case Some("printForm") => Some("Print and Post")
      case Some("phoneHMRC") => Some("Phone HMRC")
      case Some("postCode") => Some("Matched address")
      case Some("notThisAddress") => Some("Other address")
      case Some("tryAgain") => Some("Try again")
      case _ => None
    }
  }

  private def getFindMyNinoPostcodeMatched(postcodeMatched: Option[String]): Option[String] = {
    postcodeMatched match {
      case Some("true") => Some("Matched")
      case Some("false") => Some("Unmatched")
      case _ => None
    }
  }


  def buildAuditEvent(personDetails: Option[PersonalDetails],
                      auditType: String,
                      validationOutcome: String,
                      identifierType: String,
                      findMyNinoOption: Option[String],
                      findMyNinoPostcodeEntered: Option[String],
                      findMyNinoPostcodeMatched: Option[String],
                      pageErrorGeneratedFrom: Option[String],
                      errorStatus: Option[String],
                      errorReason: Option[String]
                     )(implicit hc: HeaderCarrier): ExtendedDataEvent = {
    personDetails match {
      case Some(pd) =>
        val usePostcode = pd.postCode.isDefined
        buildDataEvent(auditType, s"$auditType",
          Json.toJson(buildDetails(if (usePostcode) Some(pd.postCode.getOrElse("")) else None,
            if (!usePostcode) Some(pd.nino.nino) else None,
            getValidationOutcome(validationOutcome),
            checkCRN(identifierType),
            pd.nino.nino,
            getFindMyNinoOption(findMyNinoOption),
            findMyNinoPostcodeEntered,
            getFindMyNinoPostcodeMatched(findMyNinoPostcodeMatched),
            pageErrorGeneratedFrom,
            errorStatus,
            errorReason)))
      case None =>
        buildDataEvent(auditType, s"$auditType",
          Json.toJson(buildDetails(None,
            None,
            getValidationOutcome(validationOutcome),
            checkCRN(identifierType),
            "",
            getFindMyNinoOption(findMyNinoOption),
            findMyNinoPostcodeEntered,
            getFindMyNinoPostcodeMatched(findMyNinoPostcodeMatched),
            pageErrorGeneratedFrom,
            errorStatus,
            errorReason)))
    }
  }
}