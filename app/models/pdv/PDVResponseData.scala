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

package models.pdv

import org.apache.commons.lang3.StringUtils
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time._

case class PersonalDetails(
  firstName: String,
  lastName: String,
  nino: Nino,
  postCode: Option[String],
  dateOfBirth: LocalDate
)
object PersonalDetails {
  implicit val format: Format[PersonalDetails] = Json.format[PersonalDetails]
}

case class PDVResponseData(
  id: String,
  validationStatus: ValidationStatus,
  personalDetails: Option[PersonalDetails],
  lastUpdated: Instant = Instant.now(java.time.Clock.systemUTC()),
  reason: Option[String],
  validCustomer: Option[Boolean],
  CRN: Option[String],
  npsPostCode: Option[String]
)

object PDVResponseData {

  implicit class PDVResponseDataOps(private val pdvResponseData: PDVResponseData) extends AnyVal {
    def getPostCode: String = pdvResponseData.personalDetails match {
      case Some(pd) => pd.postCode.getOrElse(StringUtils.EMPTY)
      case _        => StringUtils.EMPTY
    }

    def getNino: String = pdvResponseData.personalDetails match {
      case Some(pd) => pd.nino.nino
      case _        => StringUtils.EMPTY
    }

    def getFirstName: String = pdvResponseData.personalDetails match {
      case Some(pd) => pd.firstName
      case _        => StringUtils.EMPTY
    }

    def getLastName: String = pdvResponseData.personalDetails match {
      case Some(pd) => pd.lastName
      case _        => StringUtils.EMPTY
    }

    def getDateOfBirth: String = pdvResponseData.personalDetails match {
      case Some(pd) => pd.dateOfBirth.toString
      case _        => StringUtils.EMPTY
    }
  }

  val reads: Reads[PDVResponseData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "id").read[String] and
        (__ \ "validationStatus").read[String].map(ValidationStatus.fromString) and
        (__ \ "personalDetails").readNullable[PersonalDetails] and
        Reads.pure(Instant.now) and
        (__ \ "reason").readNullable[String] and
        (__ \ "validCustomer").readNullable[Boolean] and
        (__ \ "CRN").readNullable[String] and
        (__ \ "npsPostCode").readNullable[String]
    )(PDVResponseData.apply _)
  }

  val writes: OWrites[PDVResponseData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "id").write[String] and
        (__ \ "validationStatus").write[String] and
        (__ \ "personalDetails").writeNullable[PersonalDetails] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat) and
        (__ \ "reason").writeNullable[String] and
        (__ \ "validCustomer").writeNullable[Boolean] and
        (__ \ "CRN").writeNullable[String] and
        (__ \ "npsPostCode").writeNullable[String]
    )((pdv: PDVResponseData) =>
      (
        pdv.id,
        pdv.validationStatus.toString,
        pdv.personalDetails,
        pdv.lastUpdated,
        pdv.reason,
        pdv.validCustomer,
        pdv.CRN,
        pdv.npsPostCode
      )
    )
  }

  implicit val format: OFormat[PDVResponseData] = OFormat(reads, writes)
}
