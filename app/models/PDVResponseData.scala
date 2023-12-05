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

package models

import org.apache.commons.lang3.StringUtils
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.domain.Nino
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset}

case class PersonalDetails(
      firstName: String,
      lastName: String,
      nino: Nino,
      dateOfBirth: LocalDate,
      postCode: Option[String]
  )
object PersonalDetails {
  implicit val format: Format[PersonalDetails] = Json.format[PersonalDetails]
}

case class PDVResponseData(
      id: String,
      validationStatus: String,
      personalDetails: Option[PersonalDetails],
      lastUpdated: Instant = LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC),
      reason: Option[String],
      validCustomer: Option[String],
      CRN: Option[String]
  )

object PDVResponseData {

  implicit class PDVResponseDataOps(private val pdvResponseData:PDVResponseData) extends AnyVal {
    def getPostCode: String = pdvResponseData.personalDetails match {
      case Some(pd) => pd.postCode.getOrElse(StringUtils.EMPTY)
      case _ => StringUtils.EMPTY
    }

  }

  val reads: Reads[PDVResponseData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "id").read[String] and
        (__ \ "validationStatus").read[String] and
        (__ \ "personalDetails").readNullable[PersonalDetails] and
          Reads.pure(Instant.now) and
        (__ \ "reason").readNullable[String] and
        (__ \ "validCustomer").readNullable[String] and
        (__ \ "CRN").readNullable[String]
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
        (__ \ "validCustomer").writeNullable[String] and
        (__ \ "CRN").writeNullable[String]
      )(unlift(PDVResponseData.unapply))
  }

  implicit val format: OFormat[PDVResponseData] = OFormat(reads, writes)
}
