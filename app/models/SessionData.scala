/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

// TODO: DDCNL-10012 After the changes for DDCNL-9796 have been live for at least a day or so the following should be done:-
/*
 Remove isOldFormat from the SessionData case class and 
 all code relating to the true value. This will be from
 this file and the DataRetrievalAction class. This is because 
 this field and program code is purely to cater for
 any Mongo documents which remain in the session cache (i.e. in 
 the existing/ old format) when these changes go live: they will 
 be gone from this short-lived cache within at most a few hours 
 (TTL is just 15 mins).
*/

case class SessionData(
  userAnswers: UserAnswers,
  origin: Option[OriginType],
  lastUpdated: Instant = Instant.now,
  id: String,
  isOldFormat: Boolean = false
)

object SessionData {
  import play.api.libs.functional.syntax._
  private val readsNewFormat: Reads[SessionData] = (
    (__ \ "userAnswers").read[UserAnswers] and
      (__ \ "origin").readNullable[OriginType] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat) and
      (__ \ "_id").read[String]
  )((ua, o, l, i) => SessionData(ua, o, l, i))

  private val readsOldFormat: Reads[SessionData] = (
    (__ \ "data").read[UserAnswers] and
      (__ \ "data" \ "origin").readNullable[String] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat) and
      (__ \ "_id").read[String]
  ) { (ua, o, lastUpdated, id) =>
    val optOriginTYpe = o.flatMap(x => OriginType.values.find(_.toString == x))
    SessionData(ua, optOriginTYpe, lastUpdated, id, isOldFormat = true)
  }

  val reads: Reads[SessionData] = Reads { js =>
    if ((js \ "userAnswers").isDefined) {
      readsNewFormat.reads(js)
    } else {
      readsOldFormat.reads(js)
    }
  }

  val writes: OWrites[SessionData] =
    (
      (__ \ "userAnswers").write[UserAnswers] and
        (__ \ "origin").writeNullable[OriginType] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat) and
        (__ \ "_id").write[String]
    ).apply(sd => Tuple4(sd.userAnswers, sd.origin, sd.lastUpdated, sd.id))

  implicit val format: OFormat[SessionData] = OFormat(reads, writes)
}
