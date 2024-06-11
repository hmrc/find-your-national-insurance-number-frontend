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

import connectors.PersonalDetailsValidationConnector
import models.pdv._
import models.requests.DataRequest
import org.mongodb.scala.MongoException
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import repositories.PersonalDetailsValidationRepoTrait
import uk.gov.hmrc.http.HeaderCarrier
import util.FMNConstants.EmptyString
import util.FMNHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PersonalDetailsValidationService @Inject()(connector: PersonalDetailsValidationConnector,
                                                 pdvRepository: PersonalDetailsValidationRepoTrait
                                                )(implicit val ec: ExecutionContext) extends Logging{

  def createPDVDataFromPDVMatch(pdvRequest: PDVRequest)(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[PDVResponse] = {
    for {
      pdvResponse <- getPDVMatchResult(pdvRequest)
      pdvResponse <- createPDVDataRow(pdvResponse)
    } yield {
      pdvResponse
    }
  }

  // Get a PDV match result
  def getPDVMatchResult(pdvRequest: PDVRequest)(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[PDVResponse] = {
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
        case INTERNAL_SERVER_ERROR =>
          logger.error("Internal server error personal-details-validation")
          PDVErrorResponse(response)
        case _ =>
          logger.error("Unexpected response from personal-details-validation")
          PDVUnexpectedResponse(response)
      }
    }
  }

  // Create a PDV data row
  def createPDVDataRow(personalDetailsValidation: PDVResponse): Future[PDVResponse] = {
    personalDetailsValidation match {
      case _@PDVSuccessResponse(pdvResponseData) =>
        (pdvResponseData.validationStatus, pdvResponseData.personalDetails) match {
          case (ValidationStatus.Success, Some(personalDetails)) =>
            val reformattedPostCode = FMNHelper.splitPostCode(personalDetails.postCode.getOrElse(EmptyString))
            if (reformattedPostCode.strip().nonEmpty) {
              val newPersonalDetails = personalDetails.copy(postCode = Some(reformattedPostCode))
              val newPDVResponseData = pdvResponseData.copy(personalDetails = Some(newPersonalDetails))
              pdvRepository.insertOrReplacePDVResultData(newPDVResponseData)
              Future.successful(PDVSuccessResponse(newPDVResponseData))
            }
            else {
              pdvRepository.insertOrReplacePDVResultData(pdvResponseData)
              Future.successful(PDVSuccessResponse(pdvResponseData))
            }
          case (ValidationStatus.Failure, None) =>
            pdvRepository.insertOrReplacePDVResultData(pdvResponseData)
            Future.successful(PDVSuccessResponse(pdvResponseData))
          case (_, None) =>
            Future.failed(new RuntimeException("PersonalDetails is None in PDVResponseData"))
        }
      case pdvNotFoundResponse@PDVNotFoundResponse(_) =>
        logger.warn(s"Failed creating PDV data row. PDV data not found.")
        Future.successful(pdvNotFoundResponse)
      case pdvBadRequestResponse@PDVBadRequestResponse(_) =>
        logger.warn(s"Failed creating PDV data row. Bad PDV request.")
        Future.successful(pdvBadRequestResponse)
      case pdvUnexpectedResponse@PDVUnexpectedResponse(_) =>
        logger.warn(s"Failed creating PDV data row. PDV unexpected response.")
        Future.successful(pdvUnexpectedResponse)
      case pdvErrorResponse@PDVErrorResponse(_) =>
        logger.warn(s"Failed creating PDV data row. PDV Internal server error.")
        Future.successful(pdvErrorResponse)
      case _ =>
        logger.warn(s"Failed creating PDV data row.")
        throw new RuntimeException(s"Failed creating PDV data row.")
    }
  }

  // Update the PDV data row with the a validCustomer which is boolean value
  def updatePDVDataRowWithValidCustomer(nino: String, isValidCustomer: Boolean, reason:String): Future[Boolean] =
    pdvRepository.updateCustomerValidityWithReason(nino, isValidCustomer, reason) map {
      case str:String => if(str.length > 8) true else false
      case _ => false
    } recover {
      case _: MongoException => false
    }

  def updatePDVDataRowWithNPSPostCode(nino: String, npsPostCode: String): Future[Boolean] =
    pdvRepository.updatePDVDataWithNPSPostCode(nino, npsPostCode) map {
      case nino: String if nino.nonEmpty => true
      case _ => false
    } recover {
      case e: MongoException =>
        logger.warn(s"Failed updating PDV data row with NPS Postcode, ${e.getMessage}")
        false
    }

  def getPersonalDetailsValidationByNino(nino: String): Future[Option[PDVResponseData]] =
    pdvRepository.findByNino(nino) map {
      case Some(pdvResponseData) => Some(pdvResponseData)
      case _ => None
    } recover {
      case e: MongoException =>
        logger.warn(s"Failed finding PDV data by NINO: $nino, ${e.getMessage}")
        None
    }

  def getValidCustomerStatus(nino: String): Future[String] = {
    getPersonalDetailsValidationByNino(nino) map {
      case Some(pdvData) => pdvData.validCustomer.getOrElse("false")
      case None => "false"
    } recover {
      case _: Exception => "false"
    }
  }

  def getPDVData(body: PDVRequest)(implicit hc: HeaderCarrier, request: DataRequest[AnyContent]): Future[PDVResponse] = {
    createPDVDataFromPDVMatch(body)
  }

}