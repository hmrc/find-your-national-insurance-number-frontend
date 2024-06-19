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

import forms.SelectAlternativeServiceFormProvider
import generators.Generators
import models.NormalMode
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.SelectAlternativeServiceView

class SelectAlternativeServiceViewSpec extends ViewBehaviours with Generators {

  val form = new SelectAlternativeServiceFormProvider()()

  override def view: HtmlFormat.Appendable =
    injector.instanceOf[SelectAlternativeServiceView].apply(form, NormalMode)(fakeRequest, messages)

  override val prefix: String = "selectAlternativeService"

  behave like pageWithTitle()

  behave like pageWithBackLink()

  behave like pageWithHeading()

  behave like pageWithContent("p", "HMRC will not tell you your National Insurance number over the phone or on webchat.")
  behave like pageWithContent("p", "We will post it to you and it will arrive within 15 working days.")

  behave like pageWithContent("legend", "What would you like to do?")

  behave like pageWithContent("label", "Print and post a form")
  behave like pageWithContent("div", "Once we have received the completed form, we can send you a letter confirming your National Insurance number.")

  behave like pageWithContent("label", "Phone HMRC")
  behave like pageWithContent("div", "You’ll need to answer some questions, then we’ll post you a letter with your number on it.")
}
