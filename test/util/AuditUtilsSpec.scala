/*
 * Copyright 2025 HM Revenue & Customs
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

import models.{AddressLine, OriginType}
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.http.{HeaderCarrier, RequestId, SessionId}
import models.pdv.PersonalDetails
import models.individualdetails.{Address, AddressPostcode, AddressSequenceNumber, AddressSource, AddressStatus, AddressType, CountryCode, DeliveryInfo, PafReference, VpaMail}
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.domain.Nino
import util.AuditUtils.YourDetailsAuditEvent

import java.time.LocalDate

class AuditUtilsSpec extends AnyWordSpec with Matchers {

  implicit val hc: HeaderCarrier = HeaderCarrier(
    sessionId = Some(SessionId("session-id")),
    requestId = Some(RequestId("request-id")),
    trueClientIp = Some("127.0.0.1"),
    trueClientPort = Some("8080"),
    deviceID = Some("device"),
    otherHeaders = Seq("path" -> "/test-path")
  )

  val personDetails: PersonalDetails = PersonalDetails(
    "John",
    "Doe",
    Nino("AA123456A"),
    Some("AB12CD"),
    LocalDate.of(1976, 3, 18)
  )

  val fakeAddress: Address = Address(
    addressSequenceNumber = AddressSequenceNumber(0),
    addressSource = Some(AddressSource.Customer),
    countryCode = CountryCode(826),
    addressType = AddressType.ResidentialAddress,
    addressStatus = Some(AddressStatus.NotDlo),
    addressStartDate = LocalDate.of(2000, 1, 1),
    addressEndDate = Some(LocalDate.of(2022, 12, 31)),
    addressLastConfirmedDate = Some(LocalDate.of(2022, 1, 1)),
    vpaMail = Some(VpaMail(1)),
    deliveryInfo = Some(DeliveryInfo("Delivery info")),
    pafReference = Some(PafReference("PAF reference")),
    addressLine1 = AddressLine("123 Fake Street"),
    addressLine2 = AddressLine("Apt 4B"),
    addressLine3 = Some(AddressLine("Faketown")),
    addressLine4 = Some(AddressLine("Fakeshire")),
    addressLine5 = Some(AddressLine("Fakecountry")),
    addressPostcode = Some(AddressPostcode("AA1 1AA"))
  )

  "buildAuditEvent" should {
    "generate correct ExtendedDataEvent with personal details and address" in {
      val event = AuditUtils.buildAuditEvent(
        personDetails = Some(personDetails),
        individualDetailsAddress = Some(fakeAddress),
        auditType = "TestAudit",
        validationOutcome = "success",
        identifierType = "true",
        findMyNinoOption = Some("postCode"),
        findMyNinoPostcodeEntered = Some("AB12CD"),
        findMyNinoPostcodeMatched = Some("true"),
        pageErrorGeneratedFrom = Some("testPage"),
        errorStatus = Some("400"),
        errorReason = Some("Bad Request"),
        origin = Some(OriginType.FMN)
      )

      event.auditSource          shouldBe "find-your-national-insurance-number-frontend"
      event.auditType            shouldBe "TestAudit"
      event.tags("X-Session-ID") shouldBe "session-id"
      event.tags("X-Request-ID") shouldBe "request-id"
      event.tags("path")         shouldBe "/test-path"

      val parsed = event.detail.as[YourDetailsAuditEvent]
      parsed.postcodeChecked                         shouldBe Some("AB12CD")
      parsed.ninoChecked                             shouldBe None
      parsed.addressLine1                            shouldBe Some("123 Fake Street")
      parsed.addressLine2                            shouldBe Some("Apt 4B")
      parsed.personalDetailsValidationOutcome        shouldBe "Matched"
      parsed.personalDetailsValidationIdentifierType shouldBe "CRN"
      parsed.identifierFromPersonalDetailsValidation shouldBe "AA123456A"
      parsed.findMyNinoOption                        shouldBe Some("Matched address")
      parsed.findMyNinoPostcodeEntered               shouldBe Some("AB12CD")
      parsed.findMyNinoPostcodeMatched               shouldBe Some("Matched")
      parsed.errorStatus                             shouldBe Some("400")
      parsed.errorReason                             shouldBe Some("Bad Request")
    }

    "generate correct ExtendedDataEvent without personal details" in {
      val event = AuditUtils.buildAuditEvent(
        personDetails = None,
        individualDetailsAddress = Some(fakeAddress),
        auditType = "TestAudit",
        validationOutcome = "failure",
        identifierType = "false",
        findMyNinoOption = Some("tryAgain"),
        findMyNinoPostcodeEntered = Some("AB12CD"),
        findMyNinoPostcodeMatched = Some("false"),
        pageErrorGeneratedFrom = None,
        errorStatus = None,
        errorReason = None,
        origin = None
      )

      val parsed = event.detail.as[YourDetailsAuditEvent]
      parsed.postcodeChecked                         shouldBe None
      parsed.ninoChecked                             shouldBe None
      parsed.personalDetailsValidationOutcome        shouldBe "Unmatched"
      parsed.personalDetailsValidationIdentifierType shouldBe "Nino"
      parsed.identifierFromPersonalDetailsValidation shouldBe ""
      parsed.findMyNinoOption                        shouldBe Some("Try again")
      parsed.findMyNinoPostcodeMatched               shouldBe Some("Unmatched")
    }
  }

  "buildBasicEvent" should {
    "generate basic ExtendedDataEvent with auditType only" in {
      val event = AuditUtils.buildBasicEvent("BasicAudit")

      event.auditType               shouldBe "BasicAudit"
      event.auditSource             shouldBe "find-your-national-insurance-number-frontend"
      event.tags("transactionName") shouldBe "BasicAudit"
      event.tags("X-Session-ID")    shouldBe "session-id"
      event.tags("X-Request-ID")    shouldBe "request-id"
      event.tags("path")            shouldBe "/test-path"
    }
  }
}
