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

package services

import com.google.inject.ImplementedBy
import models.individualdetails.{IndividualDetails, IndividualDetailsData, IndividualDetailsDataCache}
import org.mongodb.scala.MongoException
import play.api.Logging
import repositories.EncryptedIndividualDetailsRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[IndividualDetailsServiceImpl])
trait IndividualDetailsService {
  def createIndividualDetailsData(sessionId: String, individualDetails: IndividualDetails): Future[String]

  def getIndividualDetailsData(nino: String): Future[Option[IndividualDetailsDataCache]]

}

class IndividualDetailsServiceImpl @Inject()(
                                              individualDetailsRepository: EncryptedIndividualDetailsRepository
                                            )(implicit ec: ExecutionContext)
  extends IndividualDetailsService with Logging {

  override def createIndividualDetailsData(sessionId: String, individualDetails: IndividualDetails): Future[String] = {
    individualDetailsRepository.insertOrReplaceIndividualDetailsData(
      getIndividualDetailsData(sessionId, individualDetails)
    )
  }

  override def getIndividualDetailsData(nino: String): Future[Option[IndividualDetailsDataCache]] =
    individualDetailsRepository.findIndividualDetailsDataByNino(nino) map {
      case Some(individualDetailsData) => Some(individualDetailsData)
      case _ => None
    } recover({
      case e: MongoException =>
        logger.warn(s"Failed finding Individual Details Data by NINO: $nino, ${e.getMessage}")
        None
    })

  private def getIndividualDetailsData(sessionId: String, individualDetails: IndividualDetails): IndividualDetailsDataCache = {
    val iDetails = IndividualDetailsData(
      individualDetails.getFirstForename,
      individualDetails.getLastName,
      individualDetails.dateOfBirth.toString,
      individualDetails.getPostCode,
      individualDetails.getNinoWithoutSuffix
    )
    IndividualDetailsDataCache(
      sessionId,
      Some(iDetails)
    )
  }

}
