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

package viewmodels.govuk

import play.api.data.Field
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.fieldset.{Fieldset, Legend}
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.{RadioItem, Radios}
import viewmodels.ErrorMessageAwareness

object radios extends RadiosFluency

trait RadiosFluency {

  object RadiosViewModel extends ErrorMessageAwareness with FieldsetFluency {

    def apply(
      field: Field,
      items: Seq[RadioItem],
      legend: Legend
    )(implicit messages: Messages): Radios =
      apply(
        field = field,
        items = items,
        fieldset = FieldsetViewModel(legend)
      )

    def apply(
      field: Field,
      items: Seq[RadioItem],
      fieldset: Fieldset
    )(implicit messages: Messages): Radios =
      Radios(
        fieldset = Some(fieldset),
        name = field.name,
        items = items map (item => item copy (checked = field.value.isDefined && field.value == item.value)),
        errorMessage = errorMessage(field)
      )

    def yesNo(
      field: Field,
      legend: Legend
    )(implicit messages: Messages): Radios =
      yesNo(
        field = field,
        fieldset = FieldsetViewModel(legend)
      )

    def yesNoWithHint(
      field: Field,
      legend: Legend,
      yesHint: Option[String] = None,
      noHint: Option[String] = None
    )(implicit messages: Messages): Radios =
      yesNoWithHint(
        field = field,
        fieldset = FieldsetViewModel(legend),
        yesHint = yesHint,
        noHint = noHint
      )

    def yesNo(
      field: Field,
      fieldset: Fieldset
    )(implicit messages: Messages): Radios = {

      val items = Seq(
        RadioItem(
          id = Some(field.id),
          value = Some("true"),
          content = Text(messages("site.yes"))
        ),
        RadioItem(
          id = Some(s"${field.id}-no"),
          value = Some("false"),
          content = Text(messages("site.no"))
        )
      )

      apply(
        field = field,
        fieldset = fieldset,
        items = items
      )
    }

    def yesNoWithHint(
      field: Field,
      fieldset: Fieldset,
      yesHint: Option[String],
      noHint: Option[String]
    )(implicit messages: Messages): Radios = {

      val items = Seq(
        RadioItem(
          id = Some(field.id),
          value = Some("true"),
          content = Text(messages("site.yes")),
          hint = if (yesHint.isDefined) {
            Some(Hint(content = Text(yesHint.get)))
          } else {
            None
          }
        ),
        RadioItem(
          id = Some(s"${field.id}-no"),
          value = Some("false"),
          content = Text(messages("site.no")),
          hint = if (noHint.isDefined) {
            Some(Hint(content = Text(noHint.get)))
          } else {
            None
          }
        )
      )
      apply(
        field = field,
        fieldset = fieldset,
        items = items
      )
    }
  }

  implicit class FluentRadios(radios: Radios) {

    def withHint(hint: Hint): Radios =
      radios copy (hint = Some(hint))

    def withFormGroupClasses(classes: String): Radios =
      radios copy (formGroupClasses = classes)

    def withIdPrefix(prefix: String): Radios =
      radios copy (idPrefix = Some(prefix))

    def withCssClass(newClass: String): Radios =
      radios copy (classes = s"${radios.classes} $newClass")

    def withAttribute(attribute: (String, String)): Radios =
      radios copy (attributes = radios.attributes + attribute)

    def inline(): Radios =
      radios.withCssClass("govuk-radios--inline")
  }
}
