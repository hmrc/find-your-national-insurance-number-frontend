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

package connectors

import scala.collection.Seq
import cats.Show
import cats.syntax.all._
import com.codahale.metrics.MetricRegistry
import connectors.HttpReadsWrapper.showPath
import models.IndividualDetailsResponseEnvelope.IndividualDetailsResponseEnvelope
import models.errors.{IndividualDetailsError, ConnectorError}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsPath, JsResult, JsonValidationError, Reads}
import uk.gov.hmrc.http.{HttpException, HttpReads, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

final case class AdditionalLogInfo(infoKeyValue: Map[String, String]) {
  override def toString: String = {
    infoKeyValue.toList
      .map(kv => {
        val (key, value) = kv
        s"$key: $value"
      })
      .mkString(", ")
  }
}

trait HttpReadsWrapper[E, EE] { self: MetricsSupport =>
  val logger: Logger = Logger(this.getClass)
  def withHttpReads[T](name: String, registry: MetricRegistry, additionalLogInfo: Option[AdditionalLogInfo] = None)(
      block:                 HttpReads[Either[IndividualDetailsError, T]] => IndividualDetailsResponseEnvelope[T]
  )(implicit
      readsSuccess: Reads[T],
      readsError:   Reads[E],
      readsErrorT:  Reads[EE]
  ): IndividualDetailsResponseEnvelope[T] = {

    measure(name, registry) {
      block(
        getHttpReads(name: String, registry: MetricRegistry, additionalLogInfo: Option[AdditionalLogInfo])(
          readsSuccess,
          readsError,
          readsErrorT
        )
      )
    }

  }

  private def getHttpReads[T](name: String, registry: MetricRegistry, additionalLogInfo: Option[AdditionalLogInfo])(
      implicit
      readsSuccess: Reads[T],
      readsError:   Reads[E],
      readsErrorT:  Reads[EE]
  ): HttpReads[Either[IndividualDetailsError, T]] =
    (_, _, response) => {
      val additionalLogInformation = additionalLogInfo.map(ali => s"${ali.toString}, ").getOrElse("")
      response.status match {
        case Status.NO_CONTENT =>
          count(name, "success", registry)
          Right(().asInstanceOf[T])
        case Status.OK | Status.CREATED => {
          count(name, "success", registry)
          Try(response.json) match {
            case Success(value) =>
              value
                .validate[T]
                .fold(
                  error => {
                    logger
                      .debug(s"$additionalLogInformation$name couldn't parse body from upstream: error= ${error.show}")
                    ConnectorError(
                      Status.SERVICE_UNAVAILABLE,
                      s"$name couldn't parse body from upstream."
                    ).asLeft[T]
                  },
                  Right(_)
                )
            case Failure(e) => {
              logger.debug(s"$additionalLogInformation$name couldn't parse body from upstream", e)
              ConnectorError(
                Status.INTERNAL_SERVER_ERROR,
                s"$name couldn't parse error body from upstream"
              ).asLeft[T]
            }
          }
        }

        case status => {
          count(name, "failure", registry)
          Try(response.json) match {
            case Success(value) =>
              value
                .validate[E]
                .fold(
                  e => validateAdditionalError(name, status, value.validate[EE], e, additionalLogInfo),
                  error => fromUpstreamErrorToIndividualDetailsError(name, status, error, additionalLogInfo).asLeft[T]
                )
            case Failure(e) => {
              logger.debug(s"$additionalLogInformation$name couldn't parse error body from upstream", e)
              ConnectorError(
                Status.INTERNAL_SERVER_ERROR,
                s"$name couldn't parse error body from upstream"
              ).asLeft[T]
            }
          }
        }
      }
    }

  private def validateAdditionalError[T](
      name:              String,
      status:            Int,
      value:             JsResult[EE],
      validationErr:     Seq[(JsPath, Seq[JsonValidationError])],
      additionalLogInfo: Option[AdditionalLogInfo]
  ): Either[IndividualDetailsError, T] = {
    value
      .fold(
        e => validationErrorToIndividualDetailsError(name, e, additionalLogInfo),
        error =>
          fromSingleUpstreamErrorToIndividualDetailsError(name, status, error, additionalLogInfo)
            .getOrElse(validationErrorToIndividualDetailsError(name, validationErr, additionalLogInfo))
      )
      .asLeft[T]
  }

  private def validationErrorToIndividualDetailsError(
      name:              String,
      e:                 Seq[(JsPath, Seq[JsonValidationError])],
      additionalLogInfo: Option[AdditionalLogInfo]
  ): IndividualDetailsError = {
    val additionalLogInformation = additionalLogInfo.map(ali => s"${ali.toString}, ").getOrElse("")
    logger.debug(s"$additionalLogInformation$name couldn't parse error body from upstream, error= ${e.show}")
    ConnectorError(
      Status.SERVICE_UNAVAILABLE,
      s"$name couldn't parse error body from upstream"
    )
  }

  def fromUpstreamErrorToIndividualDetailsError(
      connectorName:     String,
      status:            Int,
      upstreamError:     E,
      additionalLogInfo: Option[AdditionalLogInfo]
  ): IndividualDetailsError
  def fromSingleUpstreamErrorToIndividualDetailsError(
      connectorName:     String,
      status:            Int,
      upstreamError:     EE,
      additionalLogInfo: Option[AdditionalLogInfo]
  ): Option[IndividualDetailsError]
}

object HttpReadsWrapper {
  type ParseErrors = Seq[(JsPath, Seq[JsonValidationError])]
  implicit val showPath: Show[ParseErrors] = Show.show { errors =>
    errors
      .map { error =>
        val (e, seq) = error
        s"[$e: ${seq.mkString(", ")}]"
      }
      .mkString(", ")
  }

  implicit class Recovered[T](httpResult: Future[Either[IndividualDetailsError, T]]) extends MetricsSupport {
    def recovered(
        logger:            Logger,
        connectorName:     String,
        registry:          MetricRegistry,
        additionalLogInfo: Option[AdditionalLogInfo]
    )(implicit
        ec: ExecutionContext
    ): Future[Either[IndividualDetailsError, T]] = {
      val additionalLogInformation = additionalLogInfo.map(ali => s"${ali.toString}, ").getOrElse("")

      count(connectorName, "failure", registry)
      httpResult.recover {
        case e: HttpException => {
          logger.debug(
            s"$additionalLogInformation$connectorName http exception error with response code: ${e.responseCode}",
            e
          )
          ConnectorError(e.responseCode, s"$connectorName http exception error with response code: ${e.responseCode}")
            .asLeft[T]
        }
        case e: UpstreamErrorResponse => {
          logger.debug(s"$additionalLogInformation$connectorName upstream error with status code: ${e.statusCode}", e)
          ConnectorError(e.statusCode, s"$connectorName upstream error with status code: ${e.statusCode}").asLeft[T]
        }
        case e: Throwable =>
          logger.debug(
            s"$additionalLogInformation$connectorName downstream error with status code: $INTERNAL_SERVER_ERROR",
            e
          )
          ConnectorError(
            INTERNAL_SERVER_ERROR,
            s"$connectorName downstream error with status code: $INTERNAL_SERVER_ERROR"
          ).asLeft[T]
      }
    }
  }
}
