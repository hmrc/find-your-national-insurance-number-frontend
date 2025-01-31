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
import models.{OriginType, UserAnswers}
import play.api.Logging
import play.api.libs.json.JsPath
import play.api.mvc.ActionTransformer
import queries.{Gettable, Settable}
import repositories.SessionRepository

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalImpl(sessionRepository: SessionRepository, originType: Option[OriginType])(implicit
  val executionContext: ExecutionContext
) extends DataRetrieval
    with Logging {

  private case object OriginCacheable extends Gettable[OriginType] with Settable[OriginType] {
    override def path: JsPath = JsPath \ toString

    override def toString: String = "origin"
  }

  override protected def transform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] = {
    def getOrCreateUserAnswers(optUserAnswers: Option[UserAnswers]) =
      optUserAnswers match {
        case None     =>
          UserAnswers(
            id = request.userId,
            lastUpdated = Instant.now(java.time.Clock.systemUTC())
          )
        case Some(ua) => ua
      }

    sessionRepository.get(request.userId).flatMap { optUserAnswers =>
      val futureOptionOriginType = originType match {
        case optOriginType @ Some(origin) =>
          val updatedUA = getOrCreateUserAnswers(optUserAnswers).setOrException(OriginCacheable, origin)
          sessionRepository.set(updatedUA).map(_ => Tuple2(Some(updatedUA), optOriginType))
        case None                         =>
          Future.successful(Tuple2(optUserAnswers, optUserAnswers.flatMap(_.get(OriginCacheable))))
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
