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

import models.UserAnswers
import models.requests.{DataRequest, OptionalDataRequest}
import play.api.mvc.{ActionRefiner, Result}

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

// TODO - can be removed when the CL50 journey toggle is removed
class CL50DataRequiredActionImpl @Inject()(implicit val executionContext: ExecutionContext) extends CL50DataRequiredAction {

  override protected def refine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] = {

    request.userAnswers match {
      case None =>
        val userAnswers = UserAnswers(
          id = request.userId,
          lastUpdated = Instant.now(java.time.Clock.systemUTC())
        )
        Future.successful(Right(DataRequest(request.request, request.userId, userAnswers, request.credId)))
      case Some(data) =>
        Future.successful(Right(DataRequest(request.request, request.userId, data, request.credId)))
    }
  }
}

trait CL50DataRequiredAction extends ActionRefiner[OptionalDataRequest, DataRequest]