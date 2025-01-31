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

package util

import models.{OriginType, UserAnswers}
import play.api.libs.json.JsPath
import queries.{Gettable, Settable}
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

object OriginCacheHelper {
// TODO: Change origin to enum type?
  private case object OriginCacheable extends Gettable[OriginType] with Settable[OriginType] {
    override def path: JsPath = JsPath \ toString

    override def toString: String = "origin"
  }

  def storeOrigin(origin: OriginType)(sessionRepository: SessionRepository, userAnswers: UserAnswers)(implicit ec: ExecutionContext): Future[Unit] ={
    sessionRepository.set(userAnswers.setOrException(OriginCacheable, origin)).map(_ => ():Unit)
    //origin.getOrElse("None")
  }
  
  def getOrigin(userAnswers: Option[UserAnswers]): Option[OriginType] = {
    userAnswers.flatMap(_.get(OriginCacheable))
  }
}
