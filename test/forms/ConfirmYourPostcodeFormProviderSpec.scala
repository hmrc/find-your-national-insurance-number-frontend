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

package forms

import forms.behaviours.{FormBehaviours, StringFieldBehaviours}
import models.RegexField
import play.api.data.FormError

class ConfirmYourPostcodeFormProviderSpec extends StringFieldBehaviours with FormBehaviours {

  val requiredKey = "confirmYourPostcode.error.required"
  val invalidKey = "confirmYourPostcode.error.invalid"
  val postcodeRegex = """([A-Za-z]\s*[A-HJ-Ya-hj-y]?\s*[0-9]\s*[A-Za-z0-9]?|[A-Za-z]\s*[A-HJ-Ya-hj-y]\s*[A-Za-z])\s*[0-9]\s*([ABDEFGHJLNPQRSTUWXYZabdefghjlnpqrstuwxyz]\s*){2}"""

  val validData: Map[String, String] = Map("value" -> "AA1 1ZZ")

  val form = new ConfirmYourPostcodeFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like formWithRegex(RegexField("value", invalidKey, postcodeRegex))
  }
}
