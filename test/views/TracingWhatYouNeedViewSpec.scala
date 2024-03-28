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

import generators.Generators
import play.twirl.api.HtmlFormat
import views.behaviours.ViewBehaviours
import views.html.TracingWhatYouNeedView

class TracingWhatYouNeedViewSpec extends ViewBehaviours with Generators {

  val tracingUrl = "/foo"

  override def view: HtmlFormat.Appendable =
    injector.instanceOf[TracingWhatYouNeedView].apply(tracingUrl)(fakeRequest, messages)

  override val prefix: String = "tracingWhatYouNeed"

  behave like pageWithTitle()

  behave like pageWithBackLink()

  behave like pageWithHeading()

  behave like pageWithContent("p", "We will ask you for personal details, like your:")

  behave like pageWithContent("li", "name")
  behave like pageWithContent("li", "date of birth")
  behave like pageWithContent("li", "National Insurance number (optional)")
  behave like pageWithContent("li", "postcode")

  behave like pageWithContent("p", "If we can match the details you give us with our records, then we can post you a letter with your National Insurance number on it.")
}
