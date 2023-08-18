/*
 * Copyright 2023 HM Revenue & Customs
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

package models
import cats.data.EitherT
import cats.syntax.either._
import models.errors.IndividualDetailsError

import scala.concurrent.{ExecutionContext, Future}

object IndividualDetailsResponseEnvelope {
  type IndividualDetailsResponseEnvelope[T] = EitherT[Future, IndividualDetailsError, T]

  def apply[T](value: T): IndividualDetailsResponseEnvelope[T] =
    EitherT[Future, IndividualDetailsError, T](Future.successful(value.asRight[IndividualDetailsError]))

  def apply[T, E](value: Either[IndividualDetailsError, T]): IndividualDetailsResponseEnvelope[T] =
    EitherT(Future successful value)

  def fromEitherF[E <: IndividualDetailsError, T](value: Future[Either[E, T]]): IndividualDetailsResponseEnvelope[T] = EitherT(value)

  def fromError[E <: IndividualDetailsError, T](error: E): IndividualDetailsResponseEnvelope[T] = EitherT(Future.successful(error.asLeft[T]))

  def fromF[T](value: Future[T])(implicit ec: ExecutionContext): IndividualDetailsResponseEnvelope[T] = EitherT(value.map(_.asRight[IndividualDetailsError]))
}
