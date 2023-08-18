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

package models.individualdetails

import models.json.WritesNumber
import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

sealed trait ManualCodingInd

object ManualCodingInd {
  object False extends ManualCodingInd
  object True  extends ManualCodingInd
  implicit val reads: Reads[ManualCodingInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[ManualCodingInd] = WritesNumber[ManualCodingInd] {
    case False => 0
    case True  => 1
  }
}

sealed trait ManualCodingReason

object ManualCodingReason {
  object None                   extends ManualCodingReason
  object Benefits               extends ManualCodingReason
  object UnderpaymentCollection extends ManualCodingReason
  object JsaAdjustment          extends ManualCodingReason
  object BprTransfer            extends ManualCodingReason
  object AgeRelatedAllowance    extends ManualCodingReason
  object EstPay                 extends ManualCodingReason
  object WeekBa                 extends ManualCodingReason
  object Psp                    extends ManualCodingReason
  object Fp                     extends ManualCodingReason
  object Others                 extends ManualCodingReason

  implicit val reads: Reads[ManualCodingReason] = JsPath
    .read[Int]
    .map {
      case 0  => None
      case 1  => Benefits
      case 2  => UnderpaymentCollection
      case 3  => JsaAdjustment
      case 4  => BprTransfer
      case 5  => AgeRelatedAllowance
      case 6  => EstPay
      case 7  => WeekBa
      case 8  => Psp
      case 9  => Fp
      case 10 => Others
    }

  implicit val writes: Writes[ManualCodingReason] = WritesNumber[ManualCodingReason] {
    case None                   => 0
    case Benefits               => 1
    case UnderpaymentCollection => 2
    case JsaAdjustment          => 3
    case BprTransfer            => 4
    case AgeRelatedAllowance    => 5
    case EstPay                 => 6
    case WeekBa                 => 7
    case Psp                    => 8
    case Fp                     => 9
    case Others                 => 10
  }
}

final case class ManualCodingOther(value: String) extends AnyVal

object ManualCodingOther {
  implicit val format = Json.valueFormat[ManualCodingOther]
}
sealed trait ManualCorrInd
object ManualCorrInd {
  object False extends ManualCorrInd
  object True  extends ManualCorrInd
  implicit val reads: Reads[ManualCorrInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[ManualCorrInd] = WritesNumber[ManualCorrInd] {
    case False => 0
    case True  => 1
  }
}
final case class ManualCorrReason(value: String) extends AnyVal
object ManualCorrReason {
  implicit val format = Json.valueFormat[ManualCorrReason]
}
final case class AdditionalNotes(value: String) extends AnyVal
object AdditionalNotes {
  implicit val format = Json.valueFormat[AdditionalNotes]
}
sealed trait DeceasedInd
object DeceasedInd {
  object False extends DeceasedInd
  object True  extends DeceasedInd
  implicit val reads: Reads[DeceasedInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[DeceasedInd] = WritesNumber[DeceasedInd] {
    case False => 0
    case True  => 1
  }

}
sealed trait S128Ind
object S128Ind {
  object False extends S128Ind
  object True  extends S128Ind
  implicit val reads: Reads[S128Ind] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[S128Ind] = WritesNumber[S128Ind] {
    case False => 0
    case True  => 1
  }
}
sealed trait NoAllowInd
object NoAllowInd {
  object False extends NoAllowInd
  object True  extends NoAllowInd
  implicit val reads: Reads[NoAllowInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[NoAllowInd] = WritesNumber[NoAllowInd] {
    case False => 0
    case True  => 1
  }
}
sealed trait EeaCmnwthInd
object EeaCmnwthInd {
  object False extends EeaCmnwthInd
  object True  extends EeaCmnwthInd
  implicit val reads: Reads[EeaCmnwthInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[EeaCmnwthInd] = WritesNumber[EeaCmnwthInd] {
    case False => 0
    case True  => 1
  }
}
sealed trait NoRepaymentInd
object NoRepaymentInd {
  object False extends NoRepaymentInd
  object True  extends NoRepaymentInd
  implicit val reads: Reads[NoRepaymentInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[NoRepaymentInd] = WritesNumber[NoRepaymentInd] {
    case False => 0
    case True  => 1
  }
}
sealed trait SaLinkInd
object SaLinkInd {
  object False extends SaLinkInd
  object True  extends SaLinkInd
  implicit val reads: Reads[SaLinkInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[SaLinkInd] = WritesNumber[SaLinkInd] {
    case False => 0
    case True  => 1
  }
}
sealed trait NoATSInd
object NoATSInd {
  object False extends NoATSInd
  object True  extends NoATSInd
  implicit val reads: Reads[NoATSInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[NoATSInd] = WritesNumber[NoATSInd] {
    case False => 0
    case True  => 1
  }
}
sealed trait TaxEqualBenInd
object TaxEqualBenInd {
  object False extends TaxEqualBenInd
  object True  extends TaxEqualBenInd
  implicit val reads: Reads[TaxEqualBenInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[TaxEqualBenInd] = WritesNumber[TaxEqualBenInd] {
    case False => 0
    case True  => 1
  }
}
sealed trait P2ToAgentInd
object P2ToAgentInd {
  object False extends P2ToAgentInd
  object True  extends P2ToAgentInd
  implicit val reads: Reads[P2ToAgentInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[P2ToAgentInd] = WritesNumber[P2ToAgentInd] {
    case False => 0
    case True  => 1
  }
}
sealed trait DigitallyExcludedInd
object DigitallyExcludedInd {
  object False extends DigitallyExcludedInd
  object True  extends DigitallyExcludedInd
  implicit val reads: Reads[DigitallyExcludedInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[DigitallyExcludedInd] = WritesNumber[DigitallyExcludedInd] {
    case False => 0
    case True  => 1
  }
}

sealed trait BankruptcyInd
object BankruptcyInd {
  object False extends BankruptcyInd
  object True  extends BankruptcyInd
  implicit val reads: Reads[BankruptcyInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[BankruptcyInd] = WritesNumber[BankruptcyInd] {
    case False => 0
    case True  => 1
  }
}
final case class BankruptcyFiledDate(value: LocalDate) extends AnyVal
object BankruptcyFiledDate {
  implicit val format = Json.valueFormat[BankruptcyFiledDate]
}
final case class Utr(value: String) extends AnyVal
object Utr {
  implicit val format = Json.valueFormat[Utr]
}

sealed trait AudioOutputInd
object AudioOutputInd {
  object False extends AudioOutputInd
  object True  extends AudioOutputInd
  implicit val reads: Reads[AudioOutputInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[AudioOutputInd] = WritesNumber[AudioOutputInd] {
    case False => 0
    case True  => 1
  }
}
sealed trait WelshOutputInd
object WelshOutputInd {
  object False extends WelshOutputInd
  object True  extends WelshOutputInd
  implicit val reads: Reads[WelshOutputInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[WelshOutputInd] = WritesNumber[WelshOutputInd] {
    case False => 0
    case True  => 1
  }
}
sealed trait LargePrintOutputInd
object LargePrintOutputInd {
  object False extends LargePrintOutputInd
  object True  extends LargePrintOutputInd
  implicit val reads: Reads[LargePrintOutputInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[LargePrintOutputInd] = WritesNumber[LargePrintOutputInd] {
    case False => 0
    case True  => 1
  }
}
sealed trait BrailleOutputInd
object BrailleOutputInd {
  object False extends BrailleOutputInd
  object True  extends BrailleOutputInd
  implicit val reads: Reads[BrailleOutputInd] = JsPath
    .read[Int]
    .map {
      case 0 => False
      case 1 => True
    }
  implicit val writes: Writes[BrailleOutputInd] = WritesNumber[BrailleOutputInd] {
    case False => 0
    case True  => 1
  }
}
sealed trait SpecialistBusinessArea
object SpecialistBusinessArea {
  object None             extends SpecialistBusinessArea
  object HnwuWexham       extends SpecialistBusinessArea
  object HnwuPortsmouth   extends SpecialistBusinessArea
  object HnwuBradford     extends SpecialistBusinessArea
  object HnwuCardiff      extends SpecialistBusinessArea
  object HnwuBirmingham   extends SpecialistBusinessArea
  object HnwuEastKilbride extends SpecialistBusinessArea
  object HnwuWashington   extends SpecialistBusinessArea
  object LbsLiverpool     extends SpecialistBusinessArea
  object ExpatManchester  extends SpecialistBusinessArea
  object ExpatPortsmouth  extends SpecialistBusinessArea
  object ExpatEdinburgh   extends SpecialistBusinessArea
  object ExpatWexham      extends SpecialistBusinessArea
  object ExpatWashington  extends SpecialistBusinessArea
  object PtiRbc           extends SpecialistBusinessArea

  implicit val reads: Reads[SpecialistBusinessArea] = JsPath
    .read[Int]
    .map {
      case 0  => None
      case 1  => HnwuWexham
      case 2  => HnwuPortsmouth
      case 3  => HnwuBradford
      case 4  => HnwuCardiff
      case 5  => HnwuBirmingham
      case 6  => HnwuEastKilbride
      case 7  => HnwuWashington
      case 8  => LbsLiverpool
      case 9  => ExpatManchester
      case 10 => ExpatPortsmouth
      case 11 => ExpatEdinburgh
      case 12 => ExpatWexham
      case 13 => ExpatWashington
      case 16 => PtiRbc
    }
  implicit val writes: Writes[SpecialistBusinessArea] = WritesNumber[SpecialistBusinessArea] {
    case None             => 0
    case HnwuWexham       => 1
    case HnwuPortsmouth   => 2
    case HnwuBradford     => 3
    case HnwuCardiff      => 4
    case HnwuBirmingham   => 5
    case HnwuEastKilbride => 6
    case HnwuWashington   => 7
    case LbsLiverpool     => 8
    case ExpatManchester  => 9
    case ExpatPortsmouth  => 10
    case ExpatEdinburgh   => 11
    case ExpatWexham      => 12
    case ExpatWashington  => 13
    case PtiRbc           => 16
  }
}
final case class SaStartYear(value: String) extends AnyVal
object SaStartYear {
  implicit val format = Json.valueFormat[SaStartYear]
}
final case class SaFinalYear(value: String) extends AnyVal
object SaFinalYear {
  implicit val format = Json.valueFormat[SaFinalYear]
}
final case class DigitalP2Ind(value: Int) extends AnyVal
object DigitalP2Ind {
  implicit val format = Json.valueFormat[DigitalP2Ind]
}
final case class Indicators(
    manualCodingInd:        Option[ManualCodingInd],
    manualCodingReason:     Option[ManualCodingReason],
    manualCodingOther:      Option[ManualCodingOther],
    manualCorrInd:          Option[ManualCorrInd],
    manualCorrReason:       Option[ManualCorrReason],
    additionalNotes:        Option[AdditionalNotes],
    deceasedInd:            Option[DeceasedInd],
    s128Ind:                Option[S128Ind],
    noAllowInd:             Option[NoAllowInd],
    eeaCmnwthInd:           Option[EeaCmnwthInd],
    noRepaymentInd:         Option[NoRepaymentInd],
    saLinkInd:              Option[SaLinkInd],
    noATSInd:               Option[NoATSInd],
    taxEqualBenInd:         Option[TaxEqualBenInd],
    p2ToAgentInd:           Option[P2ToAgentInd],
    digitallyExcludedInd:   Option[DigitallyExcludedInd],
    bankruptcyInd:          Option[BankruptcyInd],
    bankruptcyFiledDate:    Option[BankruptcyFiledDate],
    utr:                    Option[Utr],
    audioOutputInd:         Option[AudioOutputInd],
    welshOutputInd:         Option[WelshOutputInd],
    largePrintOutputInd:    Option[LargePrintOutputInd],
    brailleOutputInd:       Option[BrailleOutputInd],
    specialistBusinessArea: Option[SpecialistBusinessArea],
    saStartYear:            Option[SaStartYear],
    saFinalYear:            Option[SaFinalYear],
    digitalP2Ind:           Option[DigitalP2Ind]
)

object Indicators {
  implicit val format: Format[Indicators] = {
    val builder1 =
      (JsPath \ "manualCodingInd").formatNullable[ManualCodingInd] and
        (JsPath \ "manualCodingReason").formatNullable[ManualCodingReason] and
        (JsPath \ "manualCodingOther").formatNullable[ManualCodingOther] and
        (JsPath \ "manualCorrInd").formatNullable[ManualCorrInd] and
        (JsPath \ "manualCorrReason").formatNullable[ManualCorrReason] and
        (JsPath \ "additionalNotes").formatNullable[AdditionalNotes] and
        (JsPath \ "deceasedInd").formatNullable[DeceasedInd] and
        (JsPath \ "s128Ind").formatNullable[S128Ind] and
        (JsPath \ "noAllowInd").formatNullable[NoAllowInd] and
        (JsPath \ "eeaCmnwthInd").formatNullable[EeaCmnwthInd] and
        (JsPath \ "noRepaymentInd").formatNullable[NoRepaymentInd] and
        (JsPath \ "saLinkInd").formatNullable[SaLinkInd] and
        (JsPath \ "noATSInd").formatNullable[NoATSInd] and
        (JsPath \ "taxEqualBenInd").formatNullable[TaxEqualBenInd] and
        (JsPath \ "p2ToAgentInd").formatNullable[P2ToAgentInd] and
        (JsPath \ "digitallyExcludedInd").formatNullable[DigitallyExcludedInd] and
        (JsPath \ "bankruptcyInd").formatNullable[BankruptcyInd] and
        (JsPath \ "bankruptcyFiledDate").formatNullable[BankruptcyFiledDate] and
        (JsPath \ "utr").formatNullable[Utr] and
        (JsPath \ "audioOutputInd").formatNullable[AudioOutputInd] and
        (JsPath \ "welshOutputInd").formatNullable[WelshOutputInd] and
        (JsPath \ "largePrintOutputInd").formatNullable[LargePrintOutputInd]

    val builder2 =
      (JsPath \ "brailleOutputInd").formatNullable[BrailleOutputInd] and
        (JsPath \ "specialistBusinessArea").formatNullable[SpecialistBusinessArea] and
        (JsPath \ "saStartYear").formatNullable[SaStartYear] and
        (JsPath \ "saFinalYear").formatNullable[SaFinalYear] and
        (JsPath \ "digitalP2Ind").formatNullable[DigitalP2Ind]

    val format1 = builder1.apply[Indicators](
      (
          manualCodingInd:      Option[ManualCodingInd],
          manualCodingReason:   Option[ManualCodingReason],
          manualCodingOther:    Option[ManualCodingOther],
          manualCorrInd:        Option[ManualCorrInd],
          manualCorrReason:     Option[ManualCorrReason],
          additionalNotes:      Option[AdditionalNotes],
          deceasedInd:          Option[DeceasedInd],
          s128Ind:              Option[S128Ind],
          noAllowInd:           Option[NoAllowInd],
          eeaCmnwthInd:         Option[EeaCmnwthInd],
          noRepaymentInd:       Option[NoRepaymentInd],
          saLinkInd:            Option[SaLinkInd],
          noATSInd:             Option[NoATSInd],
          taxEqualBenInd:       Option[TaxEqualBenInd],
          p2ToAgentInd:         Option[P2ToAgentInd],
          digitallyExcludedInd: Option[DigitallyExcludedInd],
          bankruptcyInd:        Option[BankruptcyInd],
          bankruptcyFiledDate:  Option[BankruptcyFiledDate],
          utr:                  Option[Utr],
          audioOutputInd:       Option[AudioOutputInd],
          welshOutputInd:       Option[WelshOutputInd],
          largePrintOutputInd:  Option[LargePrintOutputInd]
      ) =>
        Indicators(
          manualCodingInd,
          manualCodingReason,
          manualCodingOther,
          manualCorrInd,
          manualCorrReason,
          additionalNotes,
          deceasedInd,
          s128Ind,
          noAllowInd,
          eeaCmnwthInd,
          noRepaymentInd,
          saLinkInd,
          noATSInd,
          taxEqualBenInd,
          p2ToAgentInd,
          digitallyExcludedInd,
          bankruptcyInd,
          bankruptcyFiledDate,
          utr,
          audioOutputInd,
          welshOutputInd,
          largePrintOutputInd,
          None,
          None,
          None,
          None,
          None
        ),
      (i: Indicators) =>
        (
          i.manualCodingInd,
          i.manualCodingReason,
          i.manualCodingOther,
          i.manualCorrInd,
          i.manualCorrReason,
          i.additionalNotes,
          i.deceasedInd,
          i.s128Ind,
          i.noAllowInd,
          i.eeaCmnwthInd,
          i.noRepaymentInd,
          i.saLinkInd,
          i.noATSInd,
          i.taxEqualBenInd,
          i.p2ToAgentInd,
          i.digitallyExcludedInd,
          i.bankruptcyInd,
          i.bankruptcyFiledDate,
          i.utr,
          i.audioOutputInd,
          i.welshOutputInd,
          i.largePrintOutputInd
        )
    )

    val format2 = builder2.apply[Indicators](
      (
          brailleOutputInd:       Option[BrailleOutputInd],
          specialistBusinessArea: Option[SpecialistBusinessArea],
          saStartYear:            Option[SaStartYear],
          saFinalYear:            Option[SaFinalYear],
          digitalP2Ind:           Option[DigitalP2Ind]
      ) =>
        Indicators(
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          None,
          brailleOutputInd,
          specialistBusinessArea,
          saStartYear,
          saFinalYear,
          digitalP2Ind
        ),
      (i: Indicators) =>
        (
          i.brailleOutputInd,
          i.specialistBusinessArea,
          i.saStartYear,
          i.saFinalYear,
          i.digitalP2Ind
        )
    )

    val reads = format1.flatMap { i1 =>
      format2.map { i2 =>
        i1.copy(
          brailleOutputInd = i2.brailleOutputInd,
          specialistBusinessArea = i2.specialistBusinessArea,
          saStartYear = i2.saStartYear,
          saFinalYear = i2.saFinalYear,
          digitalP2Ind = i2.digitalP2Ind
        )
      }
    }

    val writes = new Writes[Indicators] {
      override def writes(i1: Indicators): JsValue =
        format2
          .contramap[Indicators] { i2 =>
            i1.copy(
              brailleOutputInd = i2.brailleOutputInd,
              specialistBusinessArea = i2.specialistBusinessArea,
              saStartYear = i2.saStartYear,
              saFinalYear = i2.saFinalYear,
              digitalP2Ind = i2.digitalP2Ind
            )
          }
          .writes(i1)
    }

    Format(reads, writes)
  }
}
