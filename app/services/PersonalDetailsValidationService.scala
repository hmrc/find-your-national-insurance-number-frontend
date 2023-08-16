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
import models.{PersonalDetailsValidation, PersonalDetailsValidationResponse, PersonalDetailsValidationSuccessResponse}
import repositories.PersonalDetailsValidationRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PersonalDetailsValidationService @Inject()(connector: PersonalDetailsValidationConnector,
                                                     personalDetailsValidationRepository: PersonalDetailsValidationRepository
                                         )(implicit val ec: ExecutionContext) {

   private def fetchPersonalDetailsValidation(validationId: String)(implicit hc: HeaderCarrier): Future[PersonalDetailsValidationResponse] =
    connector.retrieveMatchingDetails(validationId)

  def createPDVFromValidationId(validationId: String)(implicit hc: HeaderCarrier): Future[String] = {
    fetchPersonalDetailsValidation(validationId).map {
      case PersonalDetailsValidationSuccessResponse(pdv) => createPersonalDetailsValidation(pdv) match {
        case Left(ex) => throw ex
        case Right(value) => value
      }
      case _ => throw new RuntimeException("Create PDV form validation failed.")
    }
  }

   private def createPersonalDetailsValidation(personalDetailsValidation: PersonalDetailsValidation)
                                     (implicit ec: ExecutionContext): Either[Exception, String] = {
    personalDetailsValidationRepository.insert(personalDetailsValidation)
     Right(personalDetailsValidation.id)
  }

  def getPersonalDetailsValidationByValidationId(validationId: String)(implicit ec: ExecutionContext): Future[Option[PersonalDetailsValidation]] = {
    personalDetailsValidationRepository.findByValidationId(validationId)
  }

  def getPersonalDetailsValidationByNino(nino: String)(implicit ec: ExecutionContext): Future[Option[PersonalDetailsValidation]] = {
    personalDetailsValidationRepository.findByNino(nino)
  }

}