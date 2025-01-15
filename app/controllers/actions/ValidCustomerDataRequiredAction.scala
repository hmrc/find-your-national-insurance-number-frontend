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

import controllers.PDVResponseHandler
import models.UserAnswers
import models.pdv.PDVRequest
import models.requests.{DataRequest, OptionalDataRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.PersonalDetailsValidationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import util.FMNConstants.EmptyString

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidCustomerDataRequiredActionImpl @Inject()(personalDetailsValidationService: PersonalDetailsValidationService, pdvResponseHandler: PDVResponseHandler)
                                                   (implicit val executionContext: ExecutionContext) extends ValidCustomerDataRequiredAction {

  override protected def refine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val pdvRequest = PDVRequest(
      credentialId = request.credId.getOrElse(""),
      sessionId = hc.sessionId.map(_.value).getOrElse(EmptyString)
    )

    personalDetailsValidationService.getPDVData(pdvRequest).flatMap { pdvResponse =>
      val nino = pdvResponseHandler.getNino(pdvResponse)
      personalDetailsValidationService.getPersonalDetailsValidationByNino(nino.getOrElse("")).map {
        case Some(pdvData) if pdvData.validCustomer.getOrElse(false) =>
          val userAnswers = request.userAnswers.getOrElse(
            UserAnswers(id = request.userId, lastUpdated = Instant.now(java.time.Clock.systemUTC()))
          )
          Right(DataRequest(request.request, request.userId, userAnswers, request.credId))

        case Some(_) =>
          Left(Redirect(controllers.routes.UnauthorisedController.onPageLoad))

        case None if request.userAnswers.isEmpty =>
          Left(Redirect(controllers.auth.routes.SignedOutController.onPageLoad).withNewSession)

        case None =>
          Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
  }
}

trait ValidCustomerDataRequiredAction extends ActionRefiner[OptionalDataRequest, DataRequest]
