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

import forms.PostLetterFormProvider
import generators.Generators
import models.NormalMode
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.PostLetterView

class PostLetterViewSpec extends ViewBehaviours with Generators {

  val form = new PostLetterFormProvider()()

  override def view: HtmlFormat.Appendable =
    injector.instanceOf[PostLetterView].apply(form, NormalMode)(fakeRequest, messages)

  override val prefix: String = "postLetter"

  behave like pageWithTitle()

  behave like pageWithBackLink()

  behave like pageWithHeading()

  behave like pageWithContent("p", "Because you are unable to prove your identity, you cannot access your National Insurance number online.")
  behave like pageWithContent("p", "You can instead use this service to get your number posted to the address HMRC has on record for you.")
  behave like pageWithContent("div", "We will not tell you your National Insurance number over the phone.")

  behave like pageWithContent("legend", "Would you like us to post your number to you?")
}
