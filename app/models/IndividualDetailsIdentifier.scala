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

import play.api.libs.json.{Format, JsPath, JsString, Reads, Writes}

import scala.util.matching.Regex

sealed trait IndividualDetailsIdentifier {
  val value: String
}
final case class IndividualDetailsNino(value: String) // NINO and CRN are used interchangeably
    extends IndividualDetailsIdentifier {
  def withoutSuffix = value.take(8)
}
final case class ChildReferenceNumber(value: String) extends IndividualDetailsIdentifier
final case class TemporaryReferenceNumber(value: String) extends IndividualDetailsIdentifier

object IndividualDetailsIdentifier {

  val NinoAndCRNRegexWithAndWithoutSuffix: Regex = """^[0-9]{2}[A-Z]{1}[0-9]{5}$""".r
  val CRNRegexWithNoSuffix: Regex                = """^[0-9]{2}[A-Z]{1}[0-9]{5}$""".r

  val TRNRegex: Regex = """^[0-9]{2}[A-Z]{1}[0-9]{5}$""".r

  implicit val reads: Reads[IndividualDetailsIdentifier] = JsPath.read[String].map {
    case NinoAndCRNRegexWithAndWithoutSuffix(nino) => IndividualDetailsNino(nino)
    case CRNRegexWithNoSuffix(crn)                 => ChildReferenceNumber(crn)
    case TRNRegex(trn)                             => TemporaryReferenceNumber(trn)
    case _                                         => throw new RuntimeException("Unable to parse ChildBenefitIdentifier")
  }

  implicit val writes: Writes[IndividualDetailsIdentifier] =
    JsPath.write[String].contramap[IndividualDetailsIdentifier] {
      case IndividualDetailsNino(nino)   => nino
      case ChildReferenceNumber(crn)     => crn
      case TemporaryReferenceNumber(trn) => trn
    }
}

object IndividualDetailsNino {
  val reads: Reads[IndividualDetailsNino]   = JsPath.read[String].map(IndividualDetailsNino.apply)
  val writes: Writes[IndividualDetailsNino] = Writes[IndividualDetailsNino](idn => JsString(idn.value))

  implicit val format: Format[IndividualDetailsNino] = Format(reads, writes)
}

object ChildReferenceNumber {
  val reads: Reads[ChildReferenceNumber]   = JsPath.read[String].map(ChildReferenceNumber.apply)
  val writes: Writes[ChildReferenceNumber] = Writes[ChildReferenceNumber](crn => JsString(crn.value))

  implicit val format: Format[ChildReferenceNumber] = Format(reads, writes)
}

object TemporaryReferenceNumber {
  val reads: Reads[TemporaryReferenceNumber]   = JsPath.read[String].map(TemporaryReferenceNumber.apply)
  val writes: Writes[TemporaryReferenceNumber] = Writes[TemporaryReferenceNumber](trn => JsString(trn.value))

  implicit val format: Format[TemporaryReferenceNumber] = Format(reads, writes)
}
