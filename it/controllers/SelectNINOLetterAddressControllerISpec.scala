
package controllers

import base.IntegrationSpecBase
import config.FrontendAppConfig
import controllers.auth.requests.UserRequest
import forms.SelectNINOLetterAddressFormProvider
import models.{Address, NormalMode, Person, PersonDetails, UserName}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, Enrolments, Nino}
import views.html.SelectNINOLetterAddressView

import java.time.LocalDate

class SelectNINOLetterAddressControllerISpec extends IntegrationSpecBase {

  val generatedPostcode = "AA1 1AA"

  val fakePersonDetails: PersonDetails = PersonDetails(
    Person(
      Some("John"),
      None,
      Some("Doe"),
      Some("JD"),
      Some("Mr"),
      None,
      Some("M"),
      Some(LocalDate.parse("1975-12-03")),
      Some(generatedNino)
    ),
    Some(
      Address(
        Some("1 Fake Street"),
        Some("Fake Town"),
        Some("Fake City"),
        Some("Fake Region"),
        None,
        Some(generatedPostcode),
        None,
        Some(LocalDate.of(2015, 3, 15)),
        None,
        Some("Residential"),
        isRls = false
      )
    ),
    None
  )

  override lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit lazy val configDecorator: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  implicit lazy val messages: Messages = MessagesImpl(Lang("en"), messagesApi).messages

  trait LocalSetup {
    def buildUserRequest[A](
                             nino: Option[Nino] = None,
                             userName: Option[UserName] = Some(UserName(Name(Some("Firstname"), Some("Lastname")))),
                             confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200,
                             personDetails: Option[PersonDetails] = Some(fakePersonDetails),
                             request: Request[A] = FakeRequest().asInstanceOf[Request[A]]
                           ): UserRequest[A] =
      UserRequest(
        nino,
        userName,
        confidenceLevel,
        personDetails,
        Enrolments(Set(Enrolment(""))),
        request
      )

    implicit val userRequest: UserRequest[AnyContentAsEmpty.type] = buildUserRequest()

    def view: SelectNINOLetterAddressView = app.injector.instanceOf[SelectNINOLetterAddressView]

    val formProvider = new SelectNINOLetterAddressFormProvider()
    val form = formProvider()

    def main: Html =
      view(
        form,
        NormalMode,
        generatedPostcode
      )(fakeRequest, messages)

    def doc: Document = Jsoup.parse(main.toString)

    def assertContainsText(doc: Document, text: String): Assertion =
      assert(doc.toString.contains(text), "\n\ntext " + text + " was not rendered on the page.\n")

    def assertContainsLink(doc: Document, text: String, href: String): Assertion =
      assert(
        doc.getElementsContainingText(text).attr("href").contains(href),
        s"\n\nLink $href was not rendered on the page\n"
      )
  }

  "Main" when {

    "rendering the view" must {

      "render the correct heading" in new LocalSetup {
        assertContainsText(doc, "Where would you like the letter sent?")
      }

      "render the correct postcode" in new LocalSetup {
        assertContainsText(doc, generatedPostcode)
      }

      "render the welsh language toggle" in new LocalSetup {
        assertContainsLink(doc, "Cymraeg", "/hmrc-frontend/language/cy")
      }
    }
  }
}
