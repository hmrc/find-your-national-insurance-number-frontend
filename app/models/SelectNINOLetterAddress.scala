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
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait SelectNINOLetterAddress

object SelectNINOLetterAddress extends Enumerable.Implicits {

  case object Postcode extends WithName("postCode") with SelectNINOLetterAddress
  case object NotThisAddress extends WithName("notThisAddress") with SelectNINOLetterAddress

  val values: Seq[SelectNINOLetterAddress] = Seq(
    Postcode, NotThisAddress
  )

  def options(implicit messages: Messages, postcode: String): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      if (value.toString.equals("postCode")) {
        RadioItem(
          content = HtmlContent(messages(s"selectNINOLetterAddress.${value.toString}") + s" <span class='govuk-!-font-weight-bold'>$postcode</span>"),
          value = Some(value.toString),
          id = Some(s"value_$index")
        )
      }
      else {
        RadioItem(
          content = Text(messages(s"selectNINOLetterAddress.${value.toString}")),
          value = Some(value.toString),
          id = Some(s"value_$index")
        )
      }
  }

  implicit val enumerable: Enumerable[SelectNINOLetterAddress] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
