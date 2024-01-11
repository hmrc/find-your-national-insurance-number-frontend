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

package repositories.encryption

import models.pdv.{PDVResponseData, PersonalDetails}
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.crypto.{EncryptedValue, SymmetricCryptoFactory}
import repositories.encryption.EncryptedValueFormat._
import uk.gov.hmrc.domain.Nino

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset}

case class EncryptedPDVResponseData(
                                     id: String,
                                     validationStatus: EncryptedValue,
                                     personalDetails: Option[EncryptedPersonalDetails],
                                     lastUpdated: Instant = LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC),
                                     reason: Option[EncryptedValue],
                                     validCustomer: Option[EncryptedValue],
                                     CRN: Option[EncryptedValue],
                                     npsPostCode: Option[EncryptedValue]
                                   )

case class EncryptedPersonalDetails(
                            firstName: EncryptedValue,
                            lastName: EncryptedValue,
                            nino: String,
                            postCode: Option[EncryptedValue],
                            dateOfBirth: EncryptedValue
                          )

object EncryptedPDVResponseData {

  val encryptedPersonalDetailsFormat: OFormat[EncryptedPersonalDetails] = {
    ((__ \ "firstName").format[EncryptedValue]
      ~ (__ \ "lastName").format[EncryptedValue]
      ~ (__ \ "nino").format[String]
      ~ (__ \ "postCode").formatNullable[EncryptedValue]
      ~ (__ \ "dateOfBirth").format[EncryptedValue]
      )(EncryptedPersonalDetails.apply, unlift(EncryptedPersonalDetails.unapply))
  }

  val encryptedPDVResponseDataFormat: OFormat[EncryptedPDVResponseData] = {
    ((__ \ "id").format[String]
      ~ (__ \ "validationStatus").format[EncryptedValue]
      ~ (__ \ "personalDetails").formatNullable[EncryptedPersonalDetails](encryptedPersonalDetailsFormat)
      ~ (__ \ "lastUpdated").format[Instant]
      ~ (__ \ "reason").formatNullable[EncryptedValue]
      ~ (__ \ "validCustomer").formatNullable[EncryptedValue]
      ~ (__ \ "CRN").formatNullable[EncryptedValue]
      ~ (__ \ "npsPostCode").formatNullable[EncryptedValue]
      )(EncryptedPDVResponseData.apply, unlift(EncryptedPDVResponseData.unapply))
  }

  def encryptField(fieldValue: String, key: String): EncryptedValue = {
    SymmetricCryptoFactory.aesGcmAdCrypto(key).encrypt(fieldValue, key)
  }

  def encrypt(pDVResponseData: PDVResponseData, key: String): EncryptedPDVResponseData = {
    def e(fieldValue: String): EncryptedValue = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).encrypt(fieldValue, key)
    }

    EncryptedPDVResponseData(
      id = pDVResponseData.id,
      validationStatus = e(pDVResponseData.validationStatus),
      personalDetails = pDVResponseData.personalDetails.map(pd => EncryptedPersonalDetails(e(pd.firstName), e(pd.lastName), pd.nino.nino, pd.postCode map e, e(pd.dateOfBirth.toString))),
      lastUpdated = pDVResponseData.lastUpdated,
      reason = pDVResponseData.reason map e,
      validCustomer = pDVResponseData.validCustomer map e,
      CRN = pDVResponseData.CRN map e,
      npsPostCode = pDVResponseData.npsPostCode map e
    )
  }

  def decrypt(encryptedRowPersonDetails: EncryptedPDVResponseData, key: String): PDVResponseData = {
    def d(field: EncryptedValue): String = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).decrypt(field, key)
    }

    PDVResponseData(
      id = encryptedRowPersonDetails.id,
      validationStatus = d(encryptedRowPersonDetails.validationStatus),
      personalDetails = encryptedRowPersonDetails.personalDetails.map(pd => PersonalDetails(d(pd.firstName), d(pd.lastName), Nino(pd.nino), pd.postCode map d, LocalDate.parse(d(pd.dateOfBirth)))),
      lastUpdated = encryptedRowPersonDetails.lastUpdated,
      reason = encryptedRowPersonDetails.reason map d,
      validCustomer = encryptedRowPersonDetails.validCustomer map d,
      CRN = encryptedRowPersonDetails.CRN map d,
      npsPostCode = encryptedRowPersonDetails.npsPostCode map d
    )
  }
}
