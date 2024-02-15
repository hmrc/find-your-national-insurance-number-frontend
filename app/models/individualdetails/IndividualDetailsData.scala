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

import play.api.libs.json.{Json, OFormat}
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
  implicit val formatIndividualDetailsCache: OFormat[IndividualDetailsData] = Json.format[IndividualDetailsData]
  implicit val formatIndividualDetailsDataCache: OFormat[IndividualDetailsDataCache] = Json.format[IndividualDetailsDataCache]

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
