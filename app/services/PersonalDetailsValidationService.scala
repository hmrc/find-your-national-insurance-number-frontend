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

package services

import connectors.PersonalDetailsValidationConnector
import models.{PDVResponse, PDVResponseData, PDVSuccessResponse}
import org.mongodb.scala.MongoException
import repositories.PersonalDetailsValidationRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PersonalDetailsValidationService @Inject()(connector: PersonalDetailsValidationConnector,
                                                 personalDetailsValidationRepository: PersonalDetailsValidationRepository
                                                )(implicit val ec: ExecutionContext) {

  def createPDVDataFromPDVMatch(validationId: String)(implicit hc:HeaderCarrier): Future[String] = {
    for {
      pdvResponse <- getPDVMatchResult(validationId)
      pdvDataRowId <-  createPDVDataRow(pdvResponse.asInstanceOf[PDVSuccessResponse].pdvResponseData)
   } yield pdvDataRowId match {
      case "" => throw new RuntimeException(s"Failed Creating PDV data for validation id: $validationId")
      case _ => pdvDataRowId
    }
  }

  private def getPDVMatchResult(validationId: String)(implicit hc:HeaderCarrier): Future[PDVResponse] =
    connector.retrieveMatchingDetails(validationId) map {
      case pdvResponse: PDVSuccessResponse => pdvResponse
      case _ => throw new RuntimeException(s"Failed getting PDV data for validation id: $validationId")
    }

  private def createPDVDataRow(personalDetailsValidation: PDVResponseData): Future[String] = {
    personalDetailsValidationRepository.insert(personalDetailsValidation) map { _ =>
      personalDetailsValidation.id } recover {
      case e: MongoException =>
        //val errorCode = e.getCode
        ""
    }
  }

  def getPersonalDetailsValidationByValidationId(validationId: String): Future[Option[PDVResponseData]] = {
    personalDetailsValidationRepository.findByValidationId(validationId)
  }

  def getPersonalDetailsValidationByNino(nino: String): Future[Option[PDVResponseData]] = {
    personalDetailsValidationRepository.findByNino(nino)
  }

}