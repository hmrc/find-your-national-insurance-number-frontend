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
import models.PersonalDetailsValidation
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonalDetailsValidationRepository @Inject()(
  mongoComponent: MongoComponent,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext) extends PlayMongoRepository[PersonalDetailsValidation](
  collectionName = "personal-details-validation",
  mongoComponent = mongoComponent,
  domainFormat = PersonalDetailsValidation.format,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("id"),
      IndexOptions().name("idIdx").unique(true)
    ),
    IndexModel(
      Indexes.ascending("personalDetails.nino"),
      IndexOptions().name("ninoIdx")
    ),
    IndexModel(
      Indexes.ascending("lastUpdated"),
      IndexOptions()
        .name("lastUpdatedIdx")
        .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
    )
  )
) with Logging {
  def insert(personalDetailsValidation: PersonalDetailsValidation)
            (implicit ec: ExecutionContext): Future[Unit] = {
    logger.info(s"Inserting one in $collectionName table")
    collection.insertOne(personalDetailsValidation).toFuture().map(_ => ())
  }

  def findByValidationId(id: String)(implicit ec: ExecutionContext): Future[Option[PersonalDetailsValidation]] = {
    collection.find(Filters.equal("id", id))
      .headOption()
  }

  def findByNino(nino: String)(implicit ec: ExecutionContext): Future[Option[PersonalDetailsValidation]] =
    collection.find(Filters.equal("personalDetails.nino", nino))
      .headOption()
}
