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

package views

import forms.ServiceIvFormProvider
import generators.Generators
import models.NormalMode
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.ServiceIvView

class ServiceIvViewSpec extends ViewBehaviours with Generators {

  val cy = "2023"
  val ny = "2024"
  val form = new ServiceIvFormProvider()()

  override def view: HtmlFormat.Appendable =
    injector.instanceOf[ServiceIvView].apply(form, cy, ny, NormalMode)(fakeRequest, messages)

  override val prefix: String = "ServiceIv"

  behave like pageWithTitle()

  behave like pageWithBackLink()

  behave like pageWithHeading()

  behave like pageWithContent("p", "This is to keep your number safe and protect you against fraud.")
  behave like pageWithContent("p", "You can use these items to help confirm your identity.")

  behave like pageWithContent("h1", "Select the items you have nearby:")

  behave like pageWithContent("label", "A valid UK passport")
  behave like pageWithContent("label", "A non-UK passport")
  behave like pageWithContent("label", "A biometric residency permit (BRP) or card")
  behave like pageWithContent("label", "Details from a tax credit claim (optional Voice ID)")
  behave like pageWithContent("label", "A payslip or P60 from 2023 to 2024")
  behave like pageWithContent("label", "Many employers will give you a P60 to show the tax you`ve paid on your salary in the tax year.")
  behave like pageWithContent("label", "A completed Self Assessment return for the last tax year")
  behave like pageWithContent("label", "I do not have any of these items")
}
