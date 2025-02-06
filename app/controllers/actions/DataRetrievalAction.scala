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
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalImpl(
  sessionRepository: SessionRepository,
  originType: Option[OriginType]
)(implicit
  val executionContext: ExecutionContext
) extends DataRetrieval
    with Logging {

  private def updateSessionData[A](
    optSessionDataFromRepository: Option[SessionData],
    request: IdentifierRequest[A]
  ): (SessionData, Boolean) = {
    (originType, optSessionDataFromRepository) match {
      case (_, None) =>
        (
          SessionData(
            userAnswers = UserAnswers(),
            origin = originType,
            lastUpdated = Instant.now(java.time.Clock.systemUTC()),
            id = request.userId
          ),
          true
        )
      case (Some(ot), Some(sd)) if !sd.origin.contains(ot) => (sd copy (origin = originType), true)
      case (_, Some(sd)) => (sd, false)
    }
  }

  private def saveSessionData(
    updatedSessionData: SessionData,
    isChanged: Boolean
  ): Future[Unit] =
    if (isChanged || updatedSessionData.isOldFormat) {
      sessionRepository.set(updatedSessionData).map(_ => (): Unit)
    } else {
      Future.successful((): Unit)
    }

  override protected def transform[A](request: IdentifierRequest[A]): Future[DataRequest[A]] =
    for {
      optSessionDataFromRepository   <- sessionRepository.get(request.userId)
      (updatedSessionData, isChanged) = updateSessionData(optSessionDataFromRepository, request)
      _                              <- saveSessionData(updatedSessionData, isChanged)
    } yield DataRequest(
      request.request,
      request.userId,
      updatedSessionData.userAnswers,
      request.credId,
      updatedSessionData.origin
    )

}

@ImplementedBy(classOf[DataRetrievalImpl])
trait DataRetrieval extends ActionTransformer[IdentifierRequest, DataRequest]

@Singleton
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
