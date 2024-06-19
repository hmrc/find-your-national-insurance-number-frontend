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

package views

import forms.ServiceIvAppFormProvider
import generators.Generators
import models.NormalMode
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.ServiceIvAppView

class ServiceIvAppViewSpec extends ViewBehaviours with Generators {

  val form = new ServiceIvAppFormProvider()()

  override def view: HtmlFormat.Appendable =
    injector.instanceOf[ServiceIvAppView].apply(form, NormalMode)(fakeRequest, messages)

  override val prefix: String = "serviceIvApp"

  behave like pageWithTitle()

  behave like pageWithBackLink()

  behave like pageWithHeading()

  behave like pageWithContent("p", "You will need to use an app and your mobile device’s camera to match your face to the picture on your passport or UK driving licence.")
  behave like pageWithContent("legend", "Can you download an app to your mobile device?")
}
