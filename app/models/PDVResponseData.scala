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

import java.time.LocalDate

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.domain.Nino

case class PersonalDetails(firstName: String, lastName: String, nino: Nino, postCode: Option[String], dateOfBirth: LocalDate)
object PersonalDetails {
  implicit val format: Format[PersonalDetails] = Json.format[PersonalDetails]
}
case class PDVResponseData(id: String, validationStatus: String, personalDetails: Option[PersonalDetails])
object PDVResponseData {
  implicit val format: Format[PDVResponseData] = Json.format[PDVResponseData]
}
