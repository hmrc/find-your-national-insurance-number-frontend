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

  def createPersonalDetailsValidationRow(validationId: String): Unit = {
    val personalDetailsValidation = getPersonalDetailsValidation(validationId)
    personalDetailsValidationService.createPersonalDetailsValidation(
      validationId,
      personalDetailsValidation // TODO pass in data from the API call
  }

  def getPersonalDetailsValidation(validationId: String):Future[PersonalDetailsValidation] = {
    for {
      personalDetailsValidationResponse <- personalDetailsValidationService.getPersonalDetailsValidation(validationId)
      personalDetailsValidation = personalDetailsValidationResponse match {
        case PersonalDetailsValidationSuccessResponse(pd) => pd
        case _ => ""
      }
    } yield personalDetailsValidation // TODO sort out return type of method
  }

}
