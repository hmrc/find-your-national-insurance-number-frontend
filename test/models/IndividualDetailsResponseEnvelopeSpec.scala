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

package models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import cats.implicits._
import models.errors.{ConnectorError, IndividualDetailsError}
class IndividualDetailsResponseEnvelopeSpec extends AnyFlatSpec with Matchers {

  "IndividualDetailsResponseEnvelope apply" should "correctly wrap a value into a right EitherT" in {
    val value = "test"
    val result = IndividualDetailsResponseEnvelope(value)
    val futureEither = result.value

    futureEither.map { either =>
      either.isRight shouldBe true
      either.getOrElse("") shouldBe value
    }
  }

  "IndividualDetailsResponseEnvelope apply" should "correctly wrap a Right value into an EitherT" in {
    val value = "test".asRight[IndividualDetailsError]
    val result = IndividualDetailsResponseEnvelope(value)
    val futureEither = result.value

    futureEither.map { either =>
      either.isRight shouldBe true
      either.getOrElse("") shouldBe value.getOrElse("")
    }
  }

  it should "correctly wrap a Left value into an EitherT" in {
    val error = ConnectorError(500, "test error").asLeft[String]
    val result = IndividualDetailsResponseEnvelope(error)
    val futureEither = result.value

    futureEither.map { either =>
      either.isLeft shouldBe true
      either.swap.getOrElse("") shouldBe error.swap.getOrElse("")
    }
  }

  "IndividualDetailsResponseEnvelope fromError" should "correctly wrap an error into a left EitherT" in {
    val error = ConnectorError(500, "test error")
    val result = IndividualDetailsResponseEnvelope.fromError(error)
    val futureEither = result.value

    futureEither.map { either =>
      either.isLeft shouldBe true
      either.swap.getOrElse(null) shouldBe error
    }
  }

  "IndividualDetailsResponseEnvelope fromF" should "correctly wrap a Future into a right EitherT" in {
    val futureValue = Future.successful("test value")
    val result = IndividualDetailsResponseEnvelope.fromF(futureValue)
    val futureEither = result.value

    futureEither.map { either =>
      either.isRight shouldBe true
      either.getOrElse(null) shouldBe "test value"
    }
  }

}