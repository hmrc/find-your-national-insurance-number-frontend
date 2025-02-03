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

import org.bson.json.JsonParseException
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class SessionData(userAnswers: UserAnswers, origin: OriginType, lastUpdated: Instant = Instant.now, id: String)

object SessionData {
  import play.api.libs.functional.syntax._
  private val readsNewFormat: Reads[SessionData] = (
    (__ \ "userAnswers").read[UserAnswers] and
      (__ \ "origin").read[OriginType] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat) and
      (__ \ "_id").read[String]
  )(SessionData.apply _)

  // TODO: Remove this old format reads once the changes have been live for a day
  private val readsOldFormat: Reads[SessionData] = (
    (__ \ "data").read[UserAnswers] and
      (__ \ "data" \ "origin").read[String] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat) and
      (__ \ "_id").read[String]
  ) { (ua, o, lastUpdated, id) =>
    val origin = OriginType.values.find(_.toString == o) match {
      case Some(origin) => origin
      case None         => throw new JsonParseException("Missing origin type")
    }
    SessionData(ua, origin, lastUpdated, id)
  }

  // TODO: Remove the old format reads once the changes have been live for a day
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
        (__ \ "origin").write[OriginType] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat) and
        (__ \ "_id").write[String]
    )(unlift(SessionData.unapply))

  implicit val format: OFormat[SessionData] = OFormat(reads, writes)
}
