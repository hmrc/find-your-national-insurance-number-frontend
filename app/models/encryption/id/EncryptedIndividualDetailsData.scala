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

package models.encryption.id

import models.encryption.EncryptedValueFormat._
import models.individualdetails.{IndividualDetailsData, IndividualDetailsDataCache}
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.crypto.{EncryptedValue, SymmetricCryptoFactory}
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat
import util.FMNConstants.EmptyString

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

case class EncryptedIndividualDetailsData(
                                  firstForename: EncryptedValue,
                                  surname: EncryptedValue,
                                  dateOfBirth: EncryptedValue,
                                  postCode: EncryptedValue,
                                  nino: String
                                )

case class EncryptedIndividualDetailsDataCache(
  id: String,
  individualDetails: Option[EncryptedIndividualDetailsData],
  lastUpdated: Instant = LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC)
)

object EncryptedIndividualDetailsDataCache {

  private val encryptedIndividualDetailsDataFormat: OFormat[EncryptedIndividualDetailsData] = {
    ((__ \ "firstForename").format[EncryptedValue]
      ~ (__ \ "surname").format[EncryptedValue]
      ~ (__ \ "dateOfBirth").format[EncryptedValue]
      ~ (__ \ "postCode").format[EncryptedValue]
      ~ (__ \ "nino").format[String]
      )(EncryptedIndividualDetailsData.apply, unlift(EncryptedIndividualDetailsData.unapply))
  }

  val encryptedIndividualDetailsDataCacheFormat: OFormat[EncryptedIndividualDetailsDataCache] = {
    ((__ \ "id").format[String]
      ~ (__ \ "individualDetails").formatNullable[EncryptedIndividualDetailsData](encryptedIndividualDetailsDataFormat)
      ~ (__ \ "lastUpdated").format[Instant](instantFormat)
      )(EncryptedIndividualDetailsDataCache.apply, unlift(EncryptedIndividualDetailsDataCache.unapply))
  }

  def encryptField(fieldValue: String, key: String): EncryptedValue = {
    SymmetricCryptoFactory.aesGcmAdCrypto(key).encrypt(fieldValue, key)
  }

  def encrypt(individualDetailsDataCache: IndividualDetailsDataCache, key: String): EncryptedIndividualDetailsDataCache = {
    def e(fieldValue: String): EncryptedValue = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).encrypt(fieldValue, key)
    }

    EncryptedIndividualDetailsDataCache(
      id = individualDetailsDataCache.id,
      individualDetails = individualDetailsDataCache.individualDetails.map {
        id =>
          EncryptedIndividualDetailsData(
            firstForename = e(id.firstForename),
            surname = e(id.surname),
            dateOfBirth = e(id.dateOfBirth),
            postCode = e(id.postCode),
            nino = id.nino
          )
      }
    )
  }

  def decrypt(encryptedIndividualDetailsDataCache: EncryptedIndividualDetailsDataCache, key: String): IndividualDetailsDataCache = {
    def d(field: EncryptedValue): String = {
      SymmetricCryptoFactory.aesGcmAdCrypto(key).decrypt(field, key)
    }

    IndividualDetailsDataCache(
      id = encryptedIndividualDetailsDataCache.id,
      individualDetails = encryptedIndividualDetailsDataCache.individualDetails.map {
        id =>
          IndividualDetailsData(
            firstForename = d(id.firstForename),
            surname = d(id.surname),
            dateOfBirth = d(id.dateOfBirth),
            postCode = d(id.postCode),
            nino = id.nino
          )
      }
    )
  }

  implicit class IndividualDetailsDataOps(private val individualDetailsData:EncryptedIndividualDetailsDataCache) extends AnyVal {

    def getNino: String = individualDetailsData.individualDetails match {
      case Some(id) => id.nino
      case _        => EmptyString
    }

    def getPostCode: String = individualDetailsData.individualDetails match {
      case Some(id) => id.postCode.value
      case _        => EmptyString
    }

    def getFirstForename: String = individualDetailsData.individualDetails match {
      case Some(id) => id.firstForename.value
      case _        => EmptyString
    }

    def getLastName: String = individualDetailsData.individualDetails match {
      case Some(id) => id.surname.value
      case _        => EmptyString
    }

    def dateOfBirth: String = individualDetailsData.individualDetails match {
      case Some(id) => id.dateOfBirth.value
      case _        => EmptyString
    }
  }
}
