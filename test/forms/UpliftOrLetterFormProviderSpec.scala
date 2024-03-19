package forms

import forms.behaviours.CheckboxFieldBehaviours
import models.UpliftOrLetter
import play.api.data.FormError

class UpliftOrLetterFormProviderSpec extends CheckboxFieldBehaviours {

  val form = new UpliftOrLetterFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "upliftOrLetter.error.required"

    behave like checkboxField[UpliftOrLetter](
      form,
      fieldName,
      validValues  = UpliftOrLetter.values,
      invalidError = FormError(s"$fieldName[0]", "error.invalid")
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }
}
