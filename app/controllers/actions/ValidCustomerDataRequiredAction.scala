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

package controllers.actions

import models.requests.DataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.PersonalDetailsValidationService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidCustomerDataRequiredActionImpl @Inject() (
  personalDetailsValidationService: PersonalDetailsValidationService
)(implicit val executionContext: ExecutionContext)
    extends ValidCustomerDataRequiredAction {

  override protected def refine[A](request: DataRequest[A]): Future[Either[Result, DataRequest[A]]] =
    personalDetailsValidationService
      .getPersonalDetailsValidationByNino(request.session.data.getOrElse("nino", ""))
      .map {
        case Some(pdvData) =>
          if (pdvData.validCustomer.getOrElse(false)) {
            Right(DataRequest(request.request, request.userId, request.userAnswers, request.credId, request.origin))
          } else {
            Left(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
          }
        case _             =>
          if (request.userAnswers.isEmpty) {
            Left(Redirect(controllers.auth.routes.SignedOutController.onPageLoad).withNewSession)
          } else {
            Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
      }
}

trait ValidCustomerDataRequiredAction extends ActionRefiner[DataRequest, DataRequest]
