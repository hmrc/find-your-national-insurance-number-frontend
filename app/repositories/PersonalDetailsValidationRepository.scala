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
import com.mongodb.client.model.Updates
import config.FrontendAppConfig
import models.PDVResponseData
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model._
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonalDetailsValidationRepository @Inject()(
                                                     mongoComponent: MongoComponent,
                                                     appConfig: FrontendAppConfig
                                                   )(implicit ec: ExecutionContext) extends PlayMongoRepository[PDVResponseData](
  collectionName = "personal-details-validation",
  mongoComponent = mongoComponent,
  domainFormat = PDVResponseData.format,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("id"),
      IndexOptions().name("idIdx").unique(true)
    ),
    IndexModel(
      Indexes.ascending("personalDetails.nino"),
      IndexOptions().name("ninoIdx").unique(true)
    ),
    IndexModel(
      Indexes.ascending("lastUpdated"),
      IndexOptions()
        .name("lastUpdatedIdx")
        .expireAfter(appConfig.cacheTtl, TimeUnit.SECONDS)
    )
  ),
  replaceIndexes = true
) with Logging {
  def insertOrReplacePDVResultData(personalDetailsValidation: PDVResponseData)
                                  (implicit ec: ExecutionContext): Future[String] = {
    logger.info(s"insert or update one in $collectionName table")
    val filter = Filters.equal("id", personalDetailsValidation.id)
    val options = ReplaceOptions().upsert(true)
    collection.replaceOne(filter, personalDetailsValidation, options)
      .toFuture()
      .map(_ => personalDetailsValidation.id) recover {
      case e: MongoWriteException if e.getCode == 11000 =>
        logger.warn(s"Error replacing or updating into $collectionName table")
        ""
    }
  }

  def updateCustomerValidityWithReason(id: String, validCustomer: Boolean, reason: String)(implicit ec: ExecutionContext) = {
    logger.info(s"Updating one in $collectionName table")
    collection.updateMany(Filters.equal("id", id),
        Updates.combine(
          Updates.set("validCustomer", validCustomer.toString),
          Updates.set("reason", reason),
          Updates.set("CRN", if(reason.contains("CRN;")) "true" else "false")))
      .toFuture()
      .map(_ => id) recover {
      case e: MongoWriteException if e.getCode == 11000 =>
        logger.warn(s"error updating $collectionName table")
        ""
    }
  }


  def findByValidationId(id: String)(implicit ec: ExecutionContext): Future[Option[PDVResponseData]] = {
    collection.find(Filters.equal("id", id))
      .toFuture()
      .recoverWith {
        case e: Throwable => {
          logger.info(s"Failed finding PDV data by validation id: $id")
          Future.failed(e)
        }
      }.map(_.headOption)
  }

  def findByNino(nino: String)(implicit ec: ExecutionContext): Future[Option[PDVResponseData]] =
    collection.find(Filters.equal("personalDetails.nino", nino))
      .toFuture()
      .recoverWith {
        case e: Throwable => {
          logger.info(s"Failed finding PDV data by NINO: $nino, ${e.getMessage}")
          Future.failed(e)
        }
      }.map(_.headOption)
}
