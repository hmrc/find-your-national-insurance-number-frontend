/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.actions

import controllers.PDVNinoExtractor
import models.UserAnswers
import models.pdv.{PDVDataRequestWithOptionalUserAnswers, PDVDataRequestWithUserAnswers}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.PersonalDetailsValidationService

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidPDVDataRequiredActionImpl @Inject()(personalDetailsValidationService: PersonalDetailsValidationService, pdvResponseHandler: PDVNinoExtractor)
                                              (implicit val executionContext: ExecutionContext) extends ValidPDVDataRequiredAction {

  override protected def refine[A](request: PDVDataRequestWithOptionalUserAnswers[A]): Future[Either[Result, PDVDataRequestWithUserAnswers[A]]] = {
    personalDetailsValidationService.getPersonalDetailsValidationByNino(pdvResponseHandler.getNino(request.pdvResponse).getOrElse("")).map {
      case Some(pdvData) =>
        if (pdvData.validCustomer.getOrElse(false)) {
          request.userAnswers match {
            case None =>
              val userAnswers = UserAnswers(
                id = request.userId,
                lastUpdated = Instant.now(java.time.Clock.systemUTC())
              )
              Right(PDVDataRequestWithUserAnswers(request.request, request.userId, request.pdvResponse, request.credId, userAnswers))
            case Some(data) =>
              Right(PDVDataRequestWithUserAnswers(request.request, request.userId, request.pdvResponse, request.credId, data))
          }
        } else {
          Left(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
        }
      case _ =>
        if (request.userAnswers.isEmpty) {
          Left(Redirect(controllers.auth.routes.SignedOutController.onPageLoad).withNewSession)
        } else {
          Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }
  }
}

trait ValidPDVDataRequiredAction extends ActionRefiner[PDVDataRequestWithOptionalUserAnswers, PDVDataRequestWithUserAnswers]
