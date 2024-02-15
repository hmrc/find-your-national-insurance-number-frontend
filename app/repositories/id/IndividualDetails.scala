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

package repositories.id

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Nino
import util.FMNConstants.EmptyString

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

case class IndividualDetailsCache(
                              firstForename: String,
                              surname: String,
                              dateOfBirth: String,
                              postCode: String,
                              nino: String
                            )

case class IndividualDetailsData(
                                  id: String,
                                  individualDetails: Option[IndividualDetailsCache],
                                  lastUpdated: Instant = LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC)
                                )

object IndividualDetailsData {
  implicit val formatIndividualDetailsCache: OFormat[IndividualDetailsCache] = Json.format[IndividualDetailsCache]
  implicit val formatIndividualDetailsData: OFormat[IndividualDetailsData] = Json.format[IndividualDetailsData]

  implicit class IndividualDetailsDataOps(private val individualDetailsData:IndividualDetailsData) extends AnyVal {

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
