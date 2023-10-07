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
import play.api.Logging
import repositories.PersonalDetailsValidationRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PersonalDetailsValidationService @Inject()(connector: PersonalDetailsValidationConnector,
                                                 personalDetailsValidationRepository: PersonalDetailsValidationRepository
                                                )(implicit val ec: ExecutionContext) extends Logging{

  def createPDVDataFromPDVMatch(validationId: String)(implicit hc:HeaderCarrier): Future[String] = {
    for {
      pdvResponse <- getPDVMatchResult(validationId)
      pdvValidationId <-  createPDVDataRow(pdvResponse.asInstanceOf[PDVSuccessResponse].pdvResponseData)
    } yield pdvValidationId match {
      case "" => throw new RuntimeException(s"Failed Creating PDV data for validation id: $validationId")
      case v => {
        logger.debug(s"Successfully created PDV data for validation id: $v")
        pdvValidationId
      } // we can possible validate that its a UUID
    }
  }


  //get a PDV match result
  private def getPDVMatchResult(validationId: String)(implicit hc:HeaderCarrier): Future[PDVResponse] =
    connector.retrieveMatchingDetails(validationId) map {
      //this can be a failed or successful match result
      case pdvResponse: PDVSuccessResponse => pdvResponse
      case _ => {
        throw new RuntimeException(s"Failed getting PDV data for validation id: $validationId")
      }
    }

  //create a PDV data row
  private def createPDVDataRow(personalDetailsValidation: PDVResponseData): Future[String] = {
    personalDetailsValidationRepository.insertOrReplacePDVResultData(personalDetailsValidation) map {
      case id => id //this is validation id
    } recover {
      case e: MongoException => {
        logger.warn(s"Failed creating PDV data row for validation id: ${personalDetailsValidation.id}, ${e.getMessage}")
        ""
      }
    }
  }

  //add a function to update the PDV data row with the a validationStatus which is boolean value
  def updatePDVDataRowWithValidationStatus(validationId: String, validationStatus: Boolean, reason:String): Future[Boolean] = {
    personalDetailsValidationRepository.updateCustomerValidityWithReason(validationId, validationStatus, reason) map {
      case str:String =>if(str.length > 8) true else false
      case _ => false
    } recover {
      case e: MongoException => {
        logger.warn(s"Failed updating PDV data row for validation id: $validationId, ${e.getMessage}")
        false
      }
    }
  }

  def getPersonalDetailsValidationByValidationId(validationId: String): Future[Option[PDVResponseData]] = {
    personalDetailsValidationRepository.findByValidationId(validationId) map {
      case Some(pdvResponseData) => Some(pdvResponseData)
      case _ => None
    } recover({
      case e: MongoException =>
        logger.warn(s"Failed finding PDV data by validationid: $validationId, ${e.getMessage}")//val errorCode = e.getCode
        None
    })
  }

  def getPersonalDetailsValidationByNino(nino: String): Future[Option[PDVResponseData]] = {
    personalDetailsValidationRepository.findByNino(nino) map {
      case Some(pdvResponseData) => Some(pdvResponseData)
      case _ => None
    } recover({
      case e: MongoException =>
        logger.warn(s"Failed finding PDV data by NINO: $nino, ${e.getMessage}")
        None
    })
  }

  def getValidCustomerStatus(nino: String): Future[String] = {
    getPersonalDetailsValidationByNino(nino) map {
      case Some(pdvData) => pdvData.validCustomer.getOrElse("false")
      case None => "false"
    } recover {
      case e: Exception => "false"
    }
  }

}