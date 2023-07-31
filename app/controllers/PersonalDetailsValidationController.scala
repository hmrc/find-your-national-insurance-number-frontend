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

package controllers

import models.{PersonalDetailsValidation, PersonalDetailsValidationResponse, PersonalDetailsValidationSuccessResponse}
import services.PersonalDetailsValidationService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class PersonalDetailsValidationController @Inject()(personalDetailsValidationService: PersonalDetailsValidationService)
                                                   (implicit headerCarrier: HeaderCarrier,
                                                    executionContext: ExecutionContext
) {

  def createPersonalDetailsValidationRow(validationId: String): Unit =
  for {
    personalDetailsValidation <- getPersonalDetailsValidation(validationId)
    result <- personalDetailsValidationService.createPersonalDetailsValidation(personalDetailsValidation) // TODO pass in data from the API call
  } yield result

  def getPersonalDetailsValidation(validationId: String):Future[PersonalDetailsValidation] =
    for {
      personalDetailsValidationResponse <- personalDetailsValidationService.getPersonalDetailsValidation(validationId)
      personalDetailsValidation = personalDetailsValidationResponse match {
        case PersonalDetailsValidationSuccessResponse(pd) => pd
      }
    } yield personalDetailsValidation // TODO sort out return type of method


}
