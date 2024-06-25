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

package models.encryption.pdv

import models.encryption.EncryptedValueFormat._
import models.pdv.{PDVResponseData, PersonalDetails, ValidationStatus}
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.crypto.{EncryptedValue, SymmetricCryptoFactory}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat

import java.time._

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

  private val encryptedPersonalDetailsFormat: OFormat[EncryptedPersonalDetails] = {
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
      ~ (__ \ "lastUpdated").format[Instant](instantFormat)
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
    def encryptStringField(fieldValue: String): EncryptedValue = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).encrypt(fieldValue, key)
    }

    def encryptBooleanField(fieldValue: Boolean): EncryptedValue = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).encrypt(fieldValue.toString, key)
    }

    EncryptedPDVResponseData(
      id = pDVResponseData.id,
      validationStatus = encryptStringField(pDVResponseData.validationStatus.toString),
      personalDetails = pDVResponseData.personalDetails.map(pd =>
        EncryptedPersonalDetails(
          encryptStringField(pd.firstName),
          encryptStringField(pd.lastName),
          pd.nino.nino,
          pd.postCode map encryptStringField,
          encryptStringField(pd.dateOfBirth.toString))
      ),
      lastUpdated = pDVResponseData.lastUpdated,
      reason = pDVResponseData.reason map encryptStringField,
      validCustomer = pDVResponseData.validCustomer map encryptBooleanField,
      CRN = pDVResponseData.CRN map encryptStringField,
      npsPostCode = pDVResponseData.npsPostCode map encryptStringField
    )
  }

  def decrypt(encryptedRowPersonDetails: EncryptedPDVResponseData, key: String): PDVResponseData = {
    def decryptStringField(field: EncryptedValue): String = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).decrypt(field, key)
    }

    def decryptBooleanField(field: EncryptedValue): Boolean = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).decrypt(field, key).toBoolean
    }

    PDVResponseData(
      id = encryptedRowPersonDetails.id,
      validationStatus = ValidationStatus.withName(decryptStringField(encryptedRowPersonDetails.validationStatus)),
      personalDetails = encryptedRowPersonDetails.personalDetails.map(pd =>
        PersonalDetails(decryptStringField(pd.firstName),
          decryptStringField(pd.lastName),
          Nino(pd.nino),
          pd.postCode map decryptStringField,
          LocalDate.parse(decryptStringField(pd.dateOfBirth)))),
      lastUpdated = encryptedRowPersonDetails.lastUpdated,
      reason = encryptedRowPersonDetails.reason map decryptStringField,
      validCustomer = encryptedRowPersonDetails.validCustomer map decryptBooleanField,
      CRN = encryptedRowPersonDetails.CRN map decryptStringField,
      npsPostCode = encryptedRowPersonDetails.npsPostCode map decryptStringField
    )
  }
}
