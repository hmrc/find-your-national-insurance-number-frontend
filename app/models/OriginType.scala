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

package models

import play.api.libs.json.{JsError, JsPath, JsSuccess, Reads}
import play.api.mvc.QueryStringBindable

sealed trait OriginType

object OriginType extends Enumerable.Implicits {
  case object PDV extends WithName("PDV") with OriginType

  case object IV extends WithName("IV") with OriginType

  case object FMN extends WithName("FMN") with OriginType

  val values: Seq[OriginType] = Seq(PDV, IV, FMN)

  def toFeedbackSource(originType: OriginType): String =
    originType match {
      case PDV => "PERSONAL_DETAILS_VALIDATION"
      case IV  => "IDENTITY_VERIFICATION"
      case _   => "FIND_MY_NINO"
    }

  private val mappings: Map[String, OriginType] = values.map(v => (v.toString, v)).toMap

  implicit val reads: Reads[OriginType] =
    JsPath.read[String].flatMap {
      case aop if mappings.keySet.contains(aop) => Reads(_ => JsSuccess(mappings.apply(aop)))
      case invalidValue                         => Reads(_ => JsError(s"Invalid administrator or practitioner type: $invalidValue"))
    }

  implicit val enumerable: Enumerable[OriginType] = Enumerable(values.map(v => v.toString -> v): _*)

  implicit def queryBinder(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[OriginType] =
    new QueryStringBindable[OriginType] {
      def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, OriginType]] =
        stringBinder.bind(key, params).flatMap {
          case Right(s) =>
            values.find(_.toString == s) match {
              case Some(ot) => Some(Right(ot))
              case _        => None
            }
          case _        => Some(Left(s"Unable to bind query parameter: $key"))
        }

      def unbind(key: String, value: OriginType): String = stringBinder.unbind(key, value.toString)
    }
}
