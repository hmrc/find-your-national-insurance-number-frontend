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
import models.requests.{DataRequest, IdentifierRequest}
import models.{OriginType, SessionData, UserAnswers}
import play.api.Logging
import play.api.mvc.ActionTransformer
import repositories.SessionRepository

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalImpl(
  sessionRepository: SessionRepository,
  originType: Option[OriginType],
  createSessionData: Boolean
)(implicit
  val executionContext: ExecutionContext
) extends DataRetrieval
    with Logging {

  private def retrieveOrCreateSessionData(
    optSessionDataFromRepository: Option[SessionData],
    id: String
  ): SessionData =
    (originType, optSessionDataFromRepository) match {
      case (_, None)           =>
        SessionData(
          userAnswers = UserAnswers(),
          origin = originType,
          lastUpdated = Instant.now(java.time.Clock.systemUTC()),
          id = id
        )
      case (Some(_), Some(sd)) => sd copy (origin = originType)
      case (None, Some(sd))    => sd
    }

  private def consolidateSessionData(
    optSessionDataFromRepository: Option[SessionData],
    id: String
  ): Future[SessionData] = {
    val sd = retrieveOrCreateSessionData(optSessionDataFromRepository, id)
    ((originType, createSessionData) match {
      case (_, true)                                 => sessionRepository.set(sd)
      case (None, _) if sd.isOldFormat               => sessionRepository.set(sd)
      case _ if optSessionDataFromRepository.isEmpty => sessionRepository.set(sd)
      case _                                         => Future.successful(false)
    }).map(_ => sd)
  }

  override protected def transform[A](request: IdentifierRequest[A]): Future[DataRequest[A]] =
    sessionRepository.get(request.userId).flatMap { optSessionDataFromRepository =>
      consolidateSessionData(optSessionDataFromRepository, request.userId).map { sd =>
        DataRequest(
          request.request,
          request.userId,
          sd.userAnswers,
          request.credId,
          sd.origin
        )
      }
    }
}

@ImplementedBy(classOf[DataRetrievalImpl])
trait DataRetrieval extends ActionTransformer[IdentifierRequest, DataRequest]

class DataRetrievalActionImpl @Inject() (
  val sessionRepository: SessionRepository
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction {
  override def apply(originType: Option[OriginType], createSessionData: Boolean): DataRetrieval =
    new DataRetrievalImpl(sessionRepository, originType, createSessionData)
}

@ImplementedBy(classOf[DataRetrievalActionImpl])
trait DataRetrievalAction {
  def apply(originType: Option[OriginType] = None, createSessionData: Boolean = false): DataRetrieval
}
