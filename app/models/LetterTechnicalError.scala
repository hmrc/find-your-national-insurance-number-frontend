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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait LetterTechnicalError

object LetterTechnicalError extends Enumerable.Implicits {

  case object TryAgain extends WithName("tryAgain") with LetterTechnicalError
  case object PrintForm extends WithName("printForm") with LetterTechnicalError
  case object PhoneHmrc extends WithName("phoneHMRC") with LetterTechnicalError

  val values: Seq[LetterTechnicalError] = Seq(
    TryAgain,
    PrintForm,
    PhoneHmrc
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case (value, index) =>
    RadioItem(
      content = Text(messages(s"technicalError.${value.toString}")),
      value = Some(value.toString),
      id = Some(s"value_$index"),
      hint = Some(
        Hint(
          content = Text(messages(s"technicalError.${value.toString}.hint"))
        )
      )
    )
  }

  implicit val enumerable: Enumerable[LetterTechnicalError] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
