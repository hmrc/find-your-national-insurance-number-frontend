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
import models.individualdetails.{Address, IndividualDetails}
import models.pdv.{PDVResponseData, PersonalDetails}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import util.AuditUtils
import util.FMNConstants.EmptyString

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class AuditService @Inject()(auditConnector: AuditConnector
                            )(implicit val ec: ExecutionContext) extends Logging {

  def audit(evt: ExtendedDataEvent)(implicit hc: HeaderCarrier): Unit =
    auditConnector.sendExtendedEvent(evt).onComplete {
      case Success(AuditResult.Success)         => logger.debug(s"Sent audit event: ${evt.toString}")
      case Failure(AuditResult.Failure(msg, _)) => logger.warn(s"Could not audit ${evt.auditType}: $msg")
      case Failure(ex)                          => logger.warn(s"Could not audit ${evt.auditType}: ${ex.getMessage}")
      case _                                    => ()
    }

  def start()(implicit hc: HeaderCarrier): Unit =
    audit(AuditUtils.buildBasicEvent(auditType = "StartFindYourNino"))
  
  def findYourNinoPDVMatchFailed(pdvData: Option[PDVResponseData], origin: Option[String])
                                (implicit headerCarrier: HeaderCarrier): Unit = {
    audit(
      AuditUtils.buildAuditEvent(
        personDetails = pdvData.flatMap(_.personalDetails),
        auditType = "FindYourNinoPDVMatchFailed",
        validationOutcome = pdvData.map(_.validationStatus).getOrElse(""),
        identifierType = EmptyString,
        origin = origin
      )
    )
  }

  def findYourNinoIdDataError(pdvData: PDVResponseData,
                              errorStatusCode: Option[String],
                              idDataError: IndividualDetailsError,
                              origin: Option[String])(implicit hc: HeaderCarrier): Unit = {
    audit(
      AuditUtils.buildAuditEvent(
        personDetails = pdvData.personalDetails,
        auditType = "FindYourNinoError",
        validationOutcome = pdvData.validationStatus,
        identifierType = EmptyString,
        pageErrorGeneratedFrom = Some("/checkDetails"),
        errorStatus = errorStatusCode,
        errorReason = Some(idDataError.errorMessage),
        origin = origin
      )
    )
  }

  def findYourNinoGetPdvDataHttpError(status: String,
                                      reason: String)(implicit hc: HeaderCarrier): Unit = {
    audit(
      AuditUtils.buildAuditEvent(
        auditType = "FindYourNinoError",
        validationOutcome = EmptyString,
        identifierType = EmptyString,
        pageErrorGeneratedFrom = Some("/checkDetails"),
        errorStatus = Some(status),
        errorReason = Some(reason)
      )
    )
  }

  def findYourNinoPDVMatched(pdvData: PDVResponseData,
                             origin: Option[String],
                             idData: IndividualDetails)(implicit hc: HeaderCarrier): Unit = {
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

  def findYourNinoTechnicalError(personalDetailsResponse: PDVResponseData,
                                 personalDetails: PersonalDetails,
                                 responseStatus: Int,
                                 responseMessage: String)(implicit hc: HeaderCarrier): Unit = {
    audit(AuditUtils.buildAuditEvent(Some(personalDetails),
      auditType = "FindYourNinoError",
      validationOutcome = personalDetailsResponse.validationStatus,
      identifierType = personalDetailsResponse.CRN.getOrElse(EmptyString),
      pageErrorGeneratedFrom = Some("/confirm-your-postcode"),
      errorStatus = Some(responseStatus.toString),
      errorReason = Some(responseMessage)
    ))
  }

  def findYourNinoConfirmPostcode(userEnteredPostCode: String,
                                  individualDetailsAddress: Option[Address],
                                  pdvData: Option[PDVResponseData],
                                  findMyNinoPostcodeMatched: Option[String])(implicit hc: HeaderCarrier): Unit = {
    audit(AuditUtils.buildAuditEvent(
      pdvData.flatMap(_.personalDetails),
      individualDetailsAddress = individualDetailsAddress,
      auditType = "FindYourNinoConfirmPostcode",
      validationOutcome = pdvData.map(_.validationStatus).getOrElse("failure"),
      identifierType = pdvData.map(_.CRN.getOrElse(EmptyString)).getOrElse(EmptyString),
      findMyNinoPostcodeEntered = Some(userEnteredPostCode),
      findMyNinoPostcodeMatched = findMyNinoPostcodeMatched
    ))
  }

  def findYourNinoOnlineLetterOption(pdvData: Option[PDVResponseData],
                                     idAddress: Address,
                                     value: String)(implicit headerCarrier: HeaderCarrier): Unit = {
    audit(AuditUtils.buildAuditEvent(pdvData.flatMap(_.personalDetails),
      individualDetailsAddress = Some(idAddress),
      auditType = "FindYourNinoOnlineLetterOption",
      validationOutcome = pdvData.map(_.validationStatus).getOrElse("failure"),
      identifierType = pdvData.map(_.CRN.getOrElse("")).getOrElse(""),
      findMyNinoOption = Some(value)
    ))
  }

  def findYourNinoError(pdvData: Option[PDVResponseData],
                        responseStatus: Option[String],
                        responseMessage: String)(implicit headerCarrier: HeaderCarrier): Unit = {
    audit(AuditUtils.buildAuditEvent(pdvData.flatMap(_.personalDetails),
      auditType = "FindYourNinoError",
      validationOutcome = pdvData.map(_.validationStatus).getOrElse("failure"),
      identifierType = pdvData.map(_.CRN.getOrElse("")).getOrElse(""),
      pageErrorGeneratedFrom = Some("/postcode"),
      errorStatus = responseStatus,
      errorReason = Some(responseMessage)
    ))
  }

}