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

package repositories

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import models.TryAgainCount
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, ReplaceOptions, Updates}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TryAgainCountRepository @Inject()(
                                                     mongoComponent: MongoComponent,
                                                     appConfig: FrontendAppConfig,
                                                     clock: Clock
                                                   )(implicit ec: ExecutionContext) extends PlayMongoRepository[TryAgainCount](
  collectionName = "try-again-count",
  mongoComponent = mongoComponent,
  domainFormat = TryAgainCount.format,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("id"),
      IndexOptions().name("idIdx").unique(true)
    ),
    IndexModel(
      Indexes.ascending("lastUpdated"),
      IndexOptions()
        .name("lastUpdatedIdx")
        .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
    )
  )
) with Logging {

  private def byId(id: String): Bson = Filters.equal("id", id)

  def insertOrIncrement(id: String)
            (implicit ec: ExecutionContext) = {

        findById(id).map {
          case Some(value: TryAgainCount) => set(value)
          case None => insert(TryAgainCount(id, 1))
        }
  }

  def set(tryAgainCount: TryAgainCount)
         (implicit ec: ExecutionContext) = {
    logger.info(s"Updating one in $collectionName table")

    val updatedCount = tryAgainCount copy (count = tryAgainCount.count + 1, lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = byId(tryAgainCount.id),
        replacement = updatedCount,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture
      .map(_ => true)
      .recover {
        case exc: MongoWriteException =>
          Left(exc.getError.getMessage)
      }
  }

  def insert(tryAgainCount: TryAgainCount)
            (implicit ec: ExecutionContext) = {
    logger.info(s"Inserting one in $collectionName table")
    collection.insertOne(tryAgainCount)
      .toFuture()
      .map(_ => Right(tryAgainCount.id))
      .recover {
        case exc: MongoWriteException =>
          Left(exc.getError.getMessage)
      }
  }

  def findById(id: String)(implicit ec: ExecutionContext): Future[Option[TryAgainCount]] = {
    collection.find(Filters.equal("id", id))
      .toFuture()
      .recoverWith { case e: Throwable => {
        Left(e);
        Future.failed(e)
      }
      }
      .map(_.headOption)
  }
}
