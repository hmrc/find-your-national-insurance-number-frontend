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

package models.individualdetails

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat
import util.FMNConstants.EmptyString

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

case class IndividualDetailsData(
                              firstForename: String,
                              surname: String,
                              dateOfBirth: String,
                              postCode: String,
                              nino: String
                              )

case class IndividualDetailsDataCache(
   id: String,
   individualDetails: Option[IndividualDetailsData],
   lastUpdated: Instant = LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC)
 )

object IndividualDetailsDataCache {
  private val individualDetailsDataFormat: OFormat[IndividualDetailsData] = {
    ((__ \ "firstForename").format[String]
      ~ (__ \ "surname").format[String]
      ~ (__ \ "dateOfBirth").format[String]
      ~ (__ \ "postCode").format[String]
      ~ (__ \ "nino").format[String]
      )(IndividualDetailsData.apply, unlift(IndividualDetailsData.unapply))
  }

  val individualDetailsDataCacheFormat: OFormat[IndividualDetailsDataCache] = {
    ((__ \ "id").format[String]
      ~ (__ \ "individualDetails").formatNullable[IndividualDetailsData](individualDetailsDataFormat)
      ~ (__ \ "lastUpdated").format[Instant](instantFormat)
      )(IndividualDetailsDataCache.apply, unlift(IndividualDetailsDataCache.unapply))
  }

  implicit class IndividualDetailsDataOps(private val individualDetailsData:IndividualDetailsDataCache) extends AnyVal {

    def getNino: String = individualDetailsData.individualDetails match {
      case Some(id) => id.nino
      case _        => EmptyString
    }

    def getPostCode: String = individualDetailsData.individualDetails match {
      case Some(id) => id.postCode
      case _        => EmptyString
    }

    def getFirstForename: String = individualDetailsData.individualDetails match {
      case Some(id) => id.firstForename
      case _        => EmptyString
    }

    def getLastName: String = individualDetailsData.individualDetails match {
      case Some(id) => id.surname
      case _        => EmptyString
    }

    def dateOfBirth: String = individualDetailsData.individualDetails match {
      case Some(id) => id.dateOfBirth
      case _        => EmptyString
    }
  }

}
