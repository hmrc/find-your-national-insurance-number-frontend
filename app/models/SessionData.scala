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

case class SessionData(
  userAnswers: UserAnswers,
  origin: Option[OriginType],
  lastUpdated: Instant = Instant.now,
  id: String
)

object SessionData {
  import play.api.libs.functional.syntax._

  val reads: Reads[SessionData] = (
    (__ \ "userAnswers").read[UserAnswers] and
      (__ \ "origin").readNullable[OriginType] and
      (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat) and
      (__ \ "_id").read[String]
  )((ua, o, l, i) => SessionData(ua, o, l, i))

  val writes: OWrites[SessionData] =
    (
      (__ \ "userAnswers").write[UserAnswers] and
        (__ \ "origin").writeNullable[OriginType] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat) and
        (__ \ "_id").write[String]
    ).apply(sd => Tuple4(sd.userAnswers, sd.origin, sd.lastUpdated, sd.id))

  implicit val format: OFormat[SessionData] = OFormat(reads, writes)
}
