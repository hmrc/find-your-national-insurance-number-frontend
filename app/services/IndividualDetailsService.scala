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
import models.individualdetails.IndividualDetails
import repositories.IndividualDetailsRepository
import repositories.id.{IndividualDetailsCache, IndividualDetailsData}
import uk.gov.hmrc.http.HeaderCarrier
import util.FMNConstants.EmptyString

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[IndividualDetailsServiceImpl])
trait IndividualDetailsService {
  def createIndividualDetailsData(sessionId: String, individualDetails: IndividualDetails): Future[String]

  def getIndividualDetailsData(nino: String): Future[Option[IndividualDetailsData]]

}

class IndividualDetailsServiceImpl @Inject()(
                                              individualDetailsRepository: IndividualDetailsRepository
                                            )(implicit ec: ExecutionContext) extends IndividualDetailsService {

  override def createIndividualDetailsData(sessionId: String, individualDetails: IndividualDetails): Future[String] = {
    individualDetailsRepository.insertOrReplaceIndividualDetailsData(
      getIndividualDetailsData(sessionId, individualDetails)
    )
  }

  override def getIndividualDetailsData(nino: String): Future[Option[IndividualDetailsData]] =
    individualDetailsRepository.findIndividualDetailsDataByNino(nino)

  private def getIndividualDetailsData(sessionId: String, individualDetails: IndividualDetails): IndividualDetailsData = {
    val iDetails = IndividualDetailsCache(
      individualDetails.getFirstForename,
      individualDetails.getLastName,
      individualDetails.dateOfBirth.toString,
      individualDetails.getPostCode,
      individualDetails.getNino
    )
    IndividualDetailsData(
      sessionId,
      Some(iDetails)
    )
  }

}
