/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait ConfirmIdentity

object ConfirmIdentity extends Enumerable.Implicits {

  case object Online extends WithName("online") with ConfirmIdentity

  case object Post extends WithName("post") with ConfirmIdentity

  val values: Map[ConfirmIdentity, Boolean] = Map(
    (Online, true),
    (Post, false)
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map { case ((key, value), index) =>
    RadioItem(
      content = Text(messages(s"confirmIdentity.${key.toString}")),
      value = Some(value.toString),
      id = Some(s"value_$index")
    )
  }.toSeq

  implicit val enumerable: Enumerable[ConfirmIdentity] =
    Enumerable(
      values.keys.toSeq.map(v => v.toString -> v): _*
    )
}
