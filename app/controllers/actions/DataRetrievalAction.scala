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

import com.google.inject.ImplementedBy
import models.requests.{IdentifierRequest, OptionalDataRequest}
import models.{OriginType, SessionData, UserAnswers}
import play.api.Logging
import play.api.mvc.ActionTransformer
import repositories.SessionRepository

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalImpl(sessionRepository: SessionRepository, originType: Option[OriginType])(implicit
  val executionContext: ExecutionContext
) extends DataRetrieval
    with Logging {

  private def consolidateSessionData(
    optSessionDataFromRepository: Option[SessionData],
    id: String
  ): Future[Option[SessionData]] = {
    val optSD = (originType, optSessionDataFromRepository) match {
      case (Some(origin), None)     =>
        Some(
          SessionData(
            userAnswers = UserAnswers(),
            origin = origin,
            lastUpdated = Instant.now(java.time.Clock.systemUTC()),
            id = id
          )
        )
      case (Some(origin), Some(sd)) => Some(sd copy (origin = origin))
      case (None, None)             => None
      case (None, Some(sd))         => Some(sd)
    }

    (originType, optSD) match {
      case (Some(_), Some(sd))                => sessionRepository.set(sd).map(_ => optSD)
      case (None, Some(sd)) if sd.isOldFormat =>
        sessionRepository.set(sd).map(_ => optSD) // If it's in the old format write back immediately in the new format
      case _                                  => Future.successful(optSD)
    }
  }

  override protected def transform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] =
    sessionRepository.get(request.userId).flatMap { optSessionDataFromRepository =>
      consolidateSessionData(optSessionDataFromRepository, request.userId).map { sd =>
        OptionalDataRequest(
          request.request,
          request.userId,
          sd.map(_.userAnswers),
          request.credId,
          sd.map(_.origin)
        )
      }
    }
}

@ImplementedBy(classOf[DataRetrievalImpl])
trait DataRetrieval extends ActionTransformer[IdentifierRequest, OptionalDataRequest]

class DataRetrievalActionImpl @Inject() (
  val sessionRepository: SessionRepository
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction {
  override def apply(originType: Option[OriginType]): DataRetrieval =
    new DataRetrievalImpl(sessionRepository, originType)
}

@ImplementedBy(classOf[DataRetrievalActionImpl])
trait DataRetrievalAction {
  def apply(originType: Option[OriginType] = None): DataRetrieval
}
