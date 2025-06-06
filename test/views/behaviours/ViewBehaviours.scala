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

package views.behaviours

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Assertion
import org.scalatest.AppendedClues.convertToClueful
import play.twirl.api.HtmlFormat
import play.twirl.api.TwirlHelperImports._
import views.base.ViewSpecAssertions

trait ViewBehaviours extends SpecBase with ViewSpecAssertions {

  def view: HtmlFormat.Appendable

  def parseView(view: HtmlFormat.Appendable): Document = Jsoup.parse(view.toString())

  lazy val doc: Document = parseView(view)

  val prefix: String

  val hasSignOutLink: Boolean = true

  if (hasSignOutLink) {
    "must render sign out link in header" in {
      val link = getElementByClass(doc, "hmrc-sign-out-nav__link")
      assertElementContainsText(link, "Sign out")
      assertElementContainsHref(
        link,
        "http://localhost:14033/find-your-national-insurance-number/account/signout?continueUrl=http://localhost:9514/feedback/FIND_MY_NINO"
      )
    }

    "must render timeout dialog" in {
      val metas = getElementsByTag(doc, "meta")
      assertElementExists(metas, _.attr("name") == "hmrc-timeout-dialog")
      assertElementExists(metas, _.attr("data-keep-alive-url").contains("refresh-session"))
    }
  } else {
    "must not render sign out link in header" in {
      assertElementDoesNotExist(doc, "hmrc-sign-out-nav__link")
    }

    "must not render timeout dialog" in {
      val metas = getElementsByTag(doc, "meta")
      assertElementDoesNotExist(metas, _.attr("name") == "hmrc-timeout-dialog")
    }
  }

  "must append service to feedback link" in {
    val link = getElementBySelector(doc, ".govuk-phase-banner__text > .govuk-link")
    getElementHref(link) must include(
      "http://localhost:9250/contact/beta-feedback?service=find-your-national-insurance-number-frontend"
    ) withClue " - GovUkBanner .get"
  }

  "must render accessibility statement link" in {
    val link = doc
      .select(".govuk-footer__inline-list-item > .govuk-footer__link")
      .toList
      .find(_.text() == "Accessibility statement")
      .get

    getElementHref(link) must include(
      "http://localhost:12346/accessibility-statement/find-your-national-insurance-number?referrerUrl="
    )
  }

  "must render 'page not working properly' link" in {
    val link = getElementByClass(doc, "hmrc-report-technical-issue")

    assertElementContainsText(link, "Is this page not working properly? (opens in new tab)")
    getElementHref(link) must include("/contact/report-technical-problem") withClue " - Technical issue"
  }

  def pageWithTitle(args: Any*): Unit =
    pageWithTitle(doc, prefix, args: _*)

  def pageWithTitle(doc: Document, prefix: String, args: Any*): Unit =
    "must render title" in {
      val title      = doc.title()
      val messageKey = s"$prefix.title"
      title mustBe s"${messages(messageKey, args: _*)} - Find your National Insurance number - GOV.UK"
      assert(messages.isDefinedAt(messageKey))
    }

  def pageWithHeading(args: Any*): Unit =
    pageWithHeading(doc, prefix, args: _*)

  def pageWithHeading(doc: Document, prefix: String, args: Any*): Unit =
    "must render heading" in {
      val heading    = getElementByTag(doc, "h1")
      val messageKey = s"$prefix.heading"
      assertElementIncludesText(heading, messages(messageKey, args: _*))
      assert(messages.isDefinedAt(messageKey))
    }

  def pageWithMatchingTitleAndHeading(args: Any*): Unit =
    pageWithMatchingTitleAndHeading(doc, prefix, args: _*)

  def pageWithMatchingTitleAndHeading(doc: Document, prefix: String, args: Any*): Unit =
    "must render title and heading" in {
      val messageKey = s"$prefix.titleAndHeading"

      val heading = getElementByTag(doc, "h1")
      assertElementIncludesText(heading, messages(messageKey, args: _*))
      assert(messages.isDefinedAt(messageKey))

      val title = doc.title()
      title mustBe s"${messages(messageKey, args: _*)} - Find your National Insurance number - GOV.UK"
      assert(messages.isDefinedAt(messageKey))
    }

  def pageWithCaption(expectedText: String): Unit =
    "must render caption" in {
      val caption = getElementByClass(doc, "govuk-caption-xl")
      assertElementContainsText(caption, expectedText)
    }

  def pageWithHint(expectedText: String): Unit =
    "must render hint" in {
      val hint = getElementByClass(doc, "govuk-hint")
      assertElementContainsText(hint, expectedText)
    }

  def pageWithoutHint(): Unit =
    "must not render hint" in {
      assertElementDoesNotExist(doc, "govuk-hint")
    }

  def pageWithSubmitButton(expectedText: String): Unit =
    pageWithButton(expectedText) { button =>
      assertElementContainsId(button, "submit")
    }

  def pageWithoutSubmitButton(): Unit =
    "must not render submit" in {
      assertElementDoesNotExist(doc, "submit")
    }

  def pageWithButton(doc: Document, expectedText: String)(additionalAssertions: Element => Assertion*): Unit =
    s"must render $expectedText button" in {
      val button = doc.getElementsByClass("govuk-button").toList.find(_.text() == expectedText).value
      additionalAssertions.map(_(button))
    }

  def pageWithLink(id: String, expectedText: String, expectedHref: String): Unit =
    pageWithLink(doc, id, expectedText, expectedHref)

  def pageWithLink(doc: Document, id: String, expectedText: String, expectedHref: String): Unit =
    s"must render link with id $id" in {
      val link = getElementById(doc, id)
      assertElementContainsText(link, expectedText)
      assertElementContainsHref(link, expectedHref)
    }

  def pageWithBackLink(): Unit =
    "must render back link" in {
      val link = getElementByClass(doc, "govuk-back-link")
      assertElementContainsText(link, "Back")
      assertElementContainsHref(link, "#")
    }

  def pageWithoutBackLink(): Unit =
    "must not render back link" in {
      assertElementDoesNotExist(doc, "govuk-back-link")
    }

  def pageWithContent(tag: String, expectedText: String): Unit =
    pageWithContent(doc, tag, expectedText)

  def pageWithContent(doc: Document, tag: String, expectedText: String): Unit =
    pageWithContent(doc, tag, expectedText, _ equals _)

  def pageWithPartialContent(tag: String, expectedText: String): Unit =
    pageWithContent(doc, tag, expectedText, _ contains _)

  private def pageWithContent(
    doc: Document,
    tag: String,
    expectedText: String,
    condition: (String, String) => Boolean
  ): Unit =
    s"must render $tag with text $expectedText" in {
      val elements = getElementsByTag(doc, tag)
      assertElementExists(elements, element => condition(element.text, expectedText))
    }

  def pageWithoutContent(doc: Document, tag: String, expectedText: String): Unit =
    s"must not render $tag with text $expectedText" in {
      val elements = getElementsByTag(doc, tag)
      assertElementDoesNotExist(elements, _.text == expectedText)
    }

  def pageWithList(listClass: String, expectedListItems: String*): Unit =
    "must render list" in {
      val list      = getElementByClass(doc, listClass)
      val listItems = list.getElementsByTag("li")
      listItems.toList.map(_.text()) mustEqual expectedListItems
    }

  def pageWithFormAction(expectedUrl: String): Unit =
    "must render form with action" in {
      val formAction = getElementByTag(doc, "form").attr("action")
      formAction mustBe expectedUrl
    }

  def pageWithoutFormAction(): Unit =
    "must render form with action" in {
      assertElementDoesNotExist(doc, "form")
    }

  def pageWithFullWidth(): Unit =
    "must have full width class" in {
      assert(doc.getElementsByClass("govuk-grid-column-full").size() == 1)
    }

  def boldWords(p: Element): Seq[String] = p.getElementsByTag("b").toList.map(_.text())

  def pageWithButton(expectedText: String)(additionalAssertions: Element => Assertion*): Unit =
    pageWithButton(doc, expectedText)(additionalAssertions: _*)
}
