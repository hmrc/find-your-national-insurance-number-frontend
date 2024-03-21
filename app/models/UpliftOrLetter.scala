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

import java.time.{LocalDate, LocalDateTime}

sealed trait UpliftOrLetter

object UpliftOrLetter extends Enumerable.Implicits {

  // select
  case object ValidUkPassport extends WithName("validUkPassport") with UpliftOrLetter
  case object UkPhotocardDrivingLicence extends WithName("ukPhotocardDrivingLicence") with UpliftOrLetter
  case object NonUkPassport extends WithName("nonUkPassport") with UpliftOrLetter
  case object UkBiometricResidenceCard extends WithName("ukBiometricResidenceCard") with UpliftOrLetter
  case object TaxCreditsClaim extends WithName("taxCreditsClaim") with UpliftOrLetter
  case object PayslipOrP60 extends WithName("payslipOrP60") with UpliftOrLetter
  case object SelfAssessment extends WithName("selfAssessment") with UpliftOrLetter

  // or
  case object NoneOfTheAbove extends WithName("noneOfTheAbove") with UpliftOrLetter

  val values: Seq[UpliftOrLetter] = Seq(
    ValidUkPassport,
    UkPhotocardDrivingLicence,
    NonUkPassport,
    UkBiometricResidenceCard,
    TaxCreditsClaim,
    PayslipOrP60,
    SelfAssessment,
    NoneOfTheAbove
  )

  val ivOptions: Seq[UpliftOrLetter] = values.filter(_ != NoneOfTheAbove)

  implicit val enumerable: Enumerable[UpliftOrLetter] = Enumerable(values.map(v => v.toString -> v): _*)

}
