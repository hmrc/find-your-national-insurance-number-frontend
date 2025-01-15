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

import models.pdv._
import models.requests.IdentifierRequest
import play.api.mvc.ActionTransformer
import repositories.SessionRepository
import services.PersonalDetailsValidationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import util.FMNConstants.EmptyString

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PDVDataRetrievalActionImpl @Inject()(
                                        personalDetailsValidationService: PersonalDetailsValidationService,
                                        sessionRepository: SessionRepository
                                      )(implicit val executionContext: ExecutionContext)
  extends PDVDataRetrievalAction {

  override protected def transform[A](request: IdentifierRequest[A]): Future[PDVDataRequestWithOptionalUserAnswers[A]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val pdvRequest = PDVRequest(
      credentialId = request.credId.getOrElse(EmptyString),
      sessionId = hc.sessionId.map(_.value).getOrElse(EmptyString)
    )

    for {
      pdvResponse <- personalDetailsValidationService.getPDVData(pdvRequest)
      userAnswers <- sessionRepository.get(request.userId)
    } yield PDVDataRequestWithOptionalUserAnswers(
      request = request.request,
      userId = request.userId,
      pdvResponse = pdvResponse,
      credId = request.credId,
      userAnswers = userAnswers
    )
  }
}

trait PDVDataRetrievalAction extends ActionTransformer[IdentifierRequest, PDVDataRequestWithOptionalUserAnswers]