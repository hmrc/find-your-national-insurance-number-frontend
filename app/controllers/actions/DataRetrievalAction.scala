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

  override protected def transform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] = {
    def getOrCreateSessionData(optSessionData: Option[SessionData], origin: OriginType): SessionData =
      optSessionData match {
        case None     =>
          SessionData(
            userAnswers = UserAnswers(),
            origin = origin,
            lastUpdated = Instant.now(java.time.Clock.systemUTC()),
            id = request.userId
          )
        case Some(sd) => sd copy (origin = origin)
      }

    sessionRepository.get(request.userId).flatMap { optSessionData =>
      val futureOptionOriginType = originType match {
        case optOriginType @ Some(origin) =>

          val sessionData = getOrCreateSessionData(optSessionData, origin)
          sessionRepository
            .set(sessionData)
            .map(_ => Tuple2(Some(sessionData.userAnswers), optOriginType))
        case None                         =>
          Future.successful(
            Tuple2(optSessionData.map(_.userAnswers), optSessionData.map(_.origin))
          )
      }

      futureOptionOriginType.map { case (optUA, optOriginType) =>
        OptionalDataRequest(
          request.request,
          request.userId,
          optUA,
          request.credId,
          optOriginType
        )
      }
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
