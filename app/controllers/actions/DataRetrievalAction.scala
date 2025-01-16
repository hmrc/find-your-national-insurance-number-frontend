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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalActionImpl @Inject()(
                                            val sessionRepository: SessionRepository,
                                             personalDetailsValidationService: PersonalDetailsValidationService
                                           )(implicit val executionContext: ExecutionContext)
  extends DataRetrievalAction {

  override protected def transform[A](request: IdentifierRequest[A]): Future[DataRequestWithOptionalUserAnswers[A]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val userAnswersF = sessionRepository.get(request.userId)
    val pdvResponseF = request.credId match {
      case Some(credId) =>
        val pdvRequest = PDVRequest(
          credentialId = credId,
          sessionId = hc.sessionId.map(_.value).getOrElse("")
        )
        personalDetailsValidationService.getPDVData(pdvRequest).map(Some(_))
      case None => Future.successful(None)
    }

    for {
      userAnswers <- userAnswersF
      pdvResponse <- pdvResponseF
    } yield DataRequestWithOptionalUserAnswers(
      request = request.request,
      userId = request.userId,
      credId = request.credId,
      userAnswers = userAnswers,
      pdvResponse = pdvResponse
    )
  }
}

trait DataRetrievalAction extends ActionTransformer[IdentifierRequest, DataRequestWithOptionalUserAnswers]