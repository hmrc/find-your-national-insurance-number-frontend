package forms

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class PostLetterFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "postLetter.error.required"
  val invalidKey = "error.boolean"

  val form = new PostLetterFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
