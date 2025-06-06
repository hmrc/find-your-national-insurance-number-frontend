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
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

case class Person(
  firstName: Option[String],
  middleName: Option[String],
  lastName: Option[String],
  initials: Option[String],
  title: Option[String],
  honours: Option[String],
  sex: Option[String],
  dateOfBirth: Option[LocalDate],
  nino: Option[Nino]
) {
  lazy val initialsName =
    initials.getOrElse(List(title, firstName.map(_.take(1)), middleName.map(_.take(1)), lastName).flatten.mkString(" "))

  lazy val shortName = for {
    f <- firstName
    l <- lastName
  } yield List(f, l).mkString(" ")

  lazy val fullName = List(title, firstName, middleName, lastName, honours).flatten.mkString(" ")
}

object Person {

  implicit class PersonOps(private val person: Person) extends AnyVal {
    def getFirstName: String   = person.firstName.getOrElse(StringUtils.EMPTY)
    def getLastName: String    = person.lastName.getOrElse(StringUtils.EMPTY)
    def getDateOfBirth: String = person.dateOfBirth.map(_.toString).getOrElse(StringUtils.EMPTY)
  }

  implicit val formats: OFormat[Person] = Json.format[Person]

}
