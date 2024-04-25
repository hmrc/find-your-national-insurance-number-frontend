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

package models

import org.apache.commons.lang3.StringUtils
import play.api.libs.json.{Json, OFormat}

case class PersonDetails(
                          person: Person,
                          address: Option[Address],
                          correspondenceAddress: Option[Address]
                        )

object PersonDetails {
  implicit class PersonDetailsOps(private val pd: PersonDetails) extends AnyVal {
    def getPostCode: String = pd.address.map(_.getPostCode).getOrElse(StringUtils.EMPTY)
  }
  implicit val formats: OFormat[PersonDetails] = Json.format[PersonDetails]
}
