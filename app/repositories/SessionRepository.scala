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

package repositories

import config.FrontendAppConfig
import models.{SessionData, UserAnswers}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/*
  Collection is named user-answers. It was originally just this but now
  it holds all session-data, not just users answers. Ideally we'd rename
  it but it would cause issues for people mid-journey so it's been left.
 */
@Singleton
class SessionRepository @Inject() (
  mongoComponent: MongoComponent,
  appConfig: FrontendAppConfig,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[SessionData](
      collectionName = "user-answers",
      mongoComponent = mongoComponent,
      domainFormat = SessionData.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
        )
      )
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  private def byId(id: String): Bson = Filters.equal("_id", id)

  def keepAlive(id: String): Future[Boolean] =
    collection
      .updateOne(
        filter = byId(id),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_ => true)

  def get(id: String): Future[Option[SessionData]] =
    keepAlive(id).flatMap { _ =>
      collection
        .find(byId(id))
        .headOption()
    }

  def set(sessionData: SessionData): Future[Boolean] = {

    val updatedSessionData = sessionData copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = byId(updatedSessionData.id),
        replacement = updatedSessionData,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => true)
  }

  def setUserAnswers(id: String, userAnswers: UserAnswers): Future[Boolean] =
    collection
      .updateOne(
        filter = byId(id),
        update = Updates.combine(
          Updates.set("userAnswers", Codecs.toBson(Json.toJson(userAnswers))),
          Updates.set("lastUpdated", Instant.now(clock))
        ),
        options = UpdateOptions().upsert(false)
      )
      .toFuture()
      .map { updateResult =>
        if (updateResult.getModifiedCount == 0) {
          throw new RuntimeException("Nothing matches in Mongo collection to update")
        } else {
          true
        }
      }

  def clear(id: String): Future[Boolean] =
    collection
      .deleteOne(byId(id))
      .toFuture()
      .map(_ => true)
}
