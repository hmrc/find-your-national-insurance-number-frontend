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

import models.errors.IndividualDetailsError
import models.individualdetails.IndividualDetails
import models.pdv.PDVResponseData
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import util.AuditUtils

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class AuditService @Inject()(auditConnector: AuditConnector, implicit val ec: ExecutionContext) extends Logging {

  def start(origin: Option[String])(implicit hc: HeaderCarrier): Unit = {
    audit(
      AuditUtils.buildAuditEvent(
        auditType = "StartFindYourNino",
        validationOutcome = "",
        identifierType = "",
        origin = origin
      )
    )
  }
  def findYourNinoPDVMatchFailed(pdvData: PDVResponseData, origin: Option[String])
                                (implicit headerCarrier: HeaderCarrier): Unit = {
    audit(
      AuditUtils.buildAuditEvent(
        personDetails = pdvData.personalDetails,
        auditType = "FindYourNinoPDVMatchFailed",
        validationOutcome = pdvData.validationStatus,
        identifierType = "",
        origin = origin
      )
    )
  }
  def findYourNinoIdDataError(pdvData: PDVResponseData, errorStatusCode: Option[String],
                              idDataError: IndividualDetailsError, origin: Option[String])(implicit headerCarrier: HeaderCarrier): Unit = {
    audit(
      AuditUtils.buildAuditEvent(
        personDetails = pdvData.personalDetails,
        auditType = "FindYourNinoError",
        validationOutcome = pdvData.validationStatus,
        identifierType = "",
        pageErrorGeneratedFrom = Some("/checkDetails"),
        errorStatus = errorStatusCode,
        errorReason = Some(idDataError.errorMessage),
        origin = origin
      )
    )
  }

  def findYourNinoGetPdvDataHttpError(status: String, reason: String)(implicit headerCarrier: HeaderCarrier): Unit = {
    audit(
      AuditUtils.buildAuditEvent(
        auditType = "FindYourNinoError",
        validationOutcome = "",
        identifierType = "",
        pageErrorGeneratedFrom = Some("/checkDetails"),
        errorStatus = Some(status),
        errorReason = Some(reason)
      )
    )
  }

  def findYourNinoPDVMatched(pdvData: PDVResponseData, origin: Option[String], idData: IndividualDetails)
                            (implicit headerCarrier: HeaderCarrier): Unit = {
    audit(
      AuditUtils.buildAuditEvent(
        personDetails = pdvData.personalDetails,
        auditType = "FindYourNinoPDVMatched",
        validationOutcome = pdvData.validationStatus,
        identifierType = idData.crnIndicator.asString,
        origin = origin
      )
    )
  }

  def audit(evt: ExtendedDataEvent)(implicit
                                    hc: HeaderCarrier
  ): Unit =
    auditConnector.sendExtendedEvent(evt).onComplete {
      case Success(AuditResult.Success)         => logger.debug(s"Sent audit event: ${evt.toString}")
      case Failure(AuditResult.Failure(msg, _)) => logger.warn(s"Could not audit ${evt.auditType}: $msg")
      case Failure(ex)                          => logger.warn(s"Could not audit ${evt.auditType}: ${ex.getMessage}")
      case _                                    => ()
    }
}