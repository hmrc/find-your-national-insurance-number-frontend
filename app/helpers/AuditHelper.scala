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

package helpers

import models.individualdetails.Address
import models.pdv.PDVResponseData
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import util.AuditUtils

import javax.inject.Inject

class AuditHelper@Inject()(auditService: AuditService) {

  def findYourNinoOnlineLetterOption(pdvData: Option[PDVResponseData], idAddress: Address, value: String)
                                    (implicit headerCarrier: HeaderCarrier): Unit = {
    auditService.audit(AuditUtils.buildAuditEvent(pdvData.flatMap(_.personalDetails),
      individualDetailsAddress = Some(idAddress),
      auditType = "FindYourNinoOnlineLetterOption",
      validationOutcome = pdvData.map(_.validationStatus).getOrElse("failure"),
      identifierType = pdvData.map(_.CRN.getOrElse("")).getOrElse(""),
      findMyNinoOption = Some(value)
    ))
  }

  def findYourNinoError(pdvData: Option[PDVResponseData], responseStatus: Int, responseMessage: String)
                       (implicit headerCarrier: HeaderCarrier): Unit = {
    auditService.audit(AuditUtils.buildAuditEvent(pdvData.flatMap(_.personalDetails),
      auditType = "FindYourNinoError",
      validationOutcome = pdvData.map(_.validationStatus).getOrElse("failure"),
      identifierType = pdvData.map(_.CRN.getOrElse("")).getOrElse(""),
      pageErrorGeneratedFrom = Some("/postcode"),
      errorStatus = Some(responseStatus.toString),
      errorReason = Some(responseMessage)
    ))
  }

}

