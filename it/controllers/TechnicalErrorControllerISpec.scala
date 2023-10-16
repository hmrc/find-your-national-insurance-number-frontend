
package controllers

import base.IntegrationSpecBase
import config.FrontendAppConfig
import controllers.auth.requests.UserRequest
import forms.TechnicalErrorServiceFormProvider
import models.{Address, NormalMode, Person, PersonDetails, TryAgainCount, UserName}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, Enrolments, Nino}
import views.html.TechnicalErrorView

import java.time.{Instant, LocalDate}

class TechnicalErrorControllerISpec extends IntegrationSpecBase {

  private val tryAgainMinCount= TryAgainCount("id", 1, Instant.ofEpochSecond(1))
  private val tryAgainMaxCount= TryAgainCount("id", 5, Instant.ofEpochSecond(1))

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
        Some("AA1 1AA"),
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

    def view: TechnicalErrorView = app.injector.instanceOf[TechnicalErrorView]

    val formProvider = new TechnicalErrorServiceFormProvider()
    val form = formProvider()

    def main: Html =
      view(
        form,
        NormalMode,
        retryAllowed = true
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
        assertContainsText(doc, "Sorry, there is a problem with the service")
      }

      "render the welsh language toggle" in new LocalSetup {
        assertContainsLink(doc, "Cymraeg", "/hmrc-frontend/language/cy")
      }

    }
  }
}
