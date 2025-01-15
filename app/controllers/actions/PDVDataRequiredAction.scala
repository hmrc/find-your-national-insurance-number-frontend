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
import models.pdv.{PDVDataRequestWithOptionalUserAnswers, PDVDataRequestWithUserAnswers}
import play.api.mvc.{ActionRefiner, Result}

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PDVDataRequiredActionImpl @Inject()(implicit val executionContext: ExecutionContext)
  extends PDVDataRequiredAction {

  override protected def refine[A](request: PDVDataRequestWithOptionalUserAnswers[A]): Future[Either[Result, PDVDataRequestWithUserAnswers[A]]] = {
    request.userAnswers match {
      case None =>
        val newUserAnswers = UserAnswers(
          id = request.userId,
          lastUpdated = Instant.now(java.time.Clock.systemUTC())
        )
        Future.successful(
          Right(
            PDVDataRequestWithUserAnswers(
              request = request.request,
              userId = request.userId,
              pdvResponse = request.pdvResponse,
              credId = request.credId,
              userAnswers = newUserAnswers
            )
          )
        )
      case Some(existingUserAnswers) =>
        Future.successful(
          Right(
            PDVDataRequestWithUserAnswers(
              request = request.request,
              userId = request.userId,
              pdvResponse = request.pdvResponse,
              credId = request.credId,
              userAnswers = existingUserAnswers
            )
          )
        )
    }
  }
}

trait PDVDataRequiredAction extends ActionRefiner[PDVDataRequestWithOptionalUserAnswers, PDVDataRequestWithUserAnswers]
