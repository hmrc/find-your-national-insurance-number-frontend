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
import models.pdv.{PDVBadRequestResponse, PDVNotFoundResponse, PDVRequest, PDVResponse, PDVResponseData, PDVSuccessResponse}
import org.mongodb.scala.MongoException
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
import play.api.libs.json.Json
import repositories.PersonalDetailsValidationRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PersonalDetailsValidationService @Inject()(connector: PersonalDetailsValidationConnector,
                                                 personalDetailsValidationRepository: PersonalDetailsValidationRepository
                                                )(implicit val ec: ExecutionContext) extends Logging{

  def createPDVDataFromPDVMatch(pdvRequest: PDVRequest)(implicit hc:HeaderCarrier): Future[PDVResponseData] =
    for {
      pdvResponse <- getPDVMatchResult(pdvRequest)
      pdvResponseData <-  createPDVDataRow(pdvResponse)
    } yield pdvResponseData


  //get a PDV match result
  private def getPDVMatchResult(pdvRequest: PDVRequest)(implicit hc:HeaderCarrier): Future[PDVResponse] =
    connector.retrieveMatchingDetails(pdvRequest) map { response =>
        response.status match {
        case OK =>
          PDVSuccessResponse(Json.parse(response.body).as[PDVResponseData])
        case BAD_REQUEST =>
          logger.warn("Bad request in personal-details-validation")
          PDVBadRequestResponse(response)
        case NOT_FOUND =>
          logger.warn("Unable to find personal details record in personal-details-validation")
          PDVNotFoundResponse(response)
        case _ =>
          throw new RuntimeException(s"Failed getting PDV data.")
      }
    }

  //create a PDV data row
  private def createPDVDataRow(personalDetailsValidation: PDVResponse): Future[PDVResponseData] = {
    personalDetailsValidation match {
      case _ @ PDVSuccessResponse(pdvResponseData) =>
        personalDetailsValidationRepository.insertOrReplacePDVResultData(pdvResponseData)
        Future.successful(pdvResponseData)
      case _ =>
        logger.warn(s"Failed creating PDV data row.")
        throw new RuntimeException(s"Failed creating PDV data row.")
    }
//    personalDetailsValidationRepository.insertOrReplacePDVResultData(personalDetailsValidation) map {
//      case id => id //this is validation id
//    } recover {
//      case e: MongoException => {
//        logger.warn(s"Failed creating PDV data row for validation id: ${personalDetailsValidation.id}, ${e.getMessage}")
//        ""
//      }
//    }
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

  def updatePDVDataRowWithNPSPostCode(nino: String, npsPostCode: String): Future[Boolean] = {
    personalDetailsValidationRepository.updatePDVDataWithNPSPostCode(nino, npsPostCode) map {
      case nino: String if nino.nonEmpty => true
      case _ => false
    } recover {
      case e: MongoException =>
        logger.warn(s"Failed updating PDV data row with NPS Postcode, ${e.getMessage}")
        false
    }
  }

//  def getPersonalDetailsValidationByValidationId(validationId: String): Future[Option[PDVResponseData]] = {
//    personalDetailsValidationRepository.findByValidationId(validationId) map {
//      case Some(pdvResponseData) => Some(pdvResponseData)
//      case _ => None
//    } recover({
//      case e: MongoException =>
//        logger.warn(s"Failed finding PDV data by validationid: $validationId, ${e.getMessage}")//val errorCode = e.getCode
//        None
//    })
//  }

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