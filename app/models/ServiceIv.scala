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

sealed trait ServiceIv

object ServiceIv extends Enumerable.Implicits {

  // select
  case object ValidUkPassport extends WithName("validUkPassport") with ServiceIv
  case object UkPhotocardDrivingLicence extends WithName("ukPhotocardDrivingLicence") with ServiceIv
  case object NonUkPassport extends WithName("nonUkPassport") with ServiceIv
  case object UkBiometricResidenceCard extends WithName("ukBiometricResidenceCard") with ServiceIv
  case object TaxCreditsClaim extends WithName("taxCreditsClaim") with ServiceIv
  case object PayslipOrP60 extends WithName("payslipOrP60") with ServiceIv
  case object SelfAssessment extends WithName("selfAssessment") with ServiceIv

  // or
  case object NoneOfTheAbove extends WithName("noneOfTheAbove") with ServiceIv

  val values: Seq[ServiceIv] = Seq(
    ValidUkPassport,
    UkPhotocardDrivingLicence,
    NonUkPassport,
    UkBiometricResidenceCard,
    TaxCreditsClaim,
    PayslipOrP60,
    SelfAssessment,
    NoneOfTheAbove
  )

  val ivOptions: Seq[ServiceIv] = values.filter(_ != NoneOfTheAbove)

  implicit val enumerable: Enumerable[ServiceIv] = Enumerable(values.map(v => v.toString -> v): _*)

}
