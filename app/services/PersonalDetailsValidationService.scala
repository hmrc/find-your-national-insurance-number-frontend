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
import models.{PersonalDetailsValidation, PersonalDetailsValidationResponse}
import repositories.PersonalDetailsValidationRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PersonalDetailsValidationService @Inject()(connector: PersonalDetailsValidationConnector,
                                                     personalDetailsValidationRepository: PersonalDetailsValidationRepository
                                         )(implicit val ec: ExecutionContext) {

  // API call to PDV to retrieve personal details
  def getPersonalDetailsValidation(validationId: String)(implicit hc: HeaderCarrier): Future[PersonalDetailsValidationResponse] =
    connector.retrieveMatchingDetails(validationId)

  // Store personal details in the mongoDB
  //def createPersonalDetailsValidation(validationId: String, validationStatus: String, personalDetails: String, dateCreated: String)
  def createPersonalDetailsValidation(pdValidation: PersonalDetailsValidation)
                                     (implicit ec: ExecutionContext): Future[Unit] = //{
                                     //(implicit ec: ExecutionContext): Either[Exception, String] = {
    // TODO change pdValidation.personalDetails type from sting to something to accomodate all fileds of PersonalDetails
    //personalDetailsValidationRepository.insert(pdValidation.id, pdValidation.validationStatus, pdValidation.personalDetails)
    // TODO change .get from `pdValidation.personalDetails.get.firstName`
    personalDetailsValidationRepository.insert(pdValidation.id, pdValidation.validationStatus, pdValidation.personalDetails.get.firstName)
    //Right(pdValidation.id)
  //}

  // Get personal details from db by id
  def getPersonalDetailsValidationByValidationId(validationId: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    personalDetailsValidationRepository.findByValidationId(validationId).map(_.map(_.personalDetails))
  }

  // Get personal details from db by nino
  def getPersonalDetailsValidationByNino(nino: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    personalDetailsValidationRepository.findByNino(nino).map(_.map(_.personalDetails))
  }

}