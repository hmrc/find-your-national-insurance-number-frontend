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

package controllers

import base.SpecBase
import forms.SelectAlternativeServiceFormProvider
import models.pdv.{PDVResponseData, PersonalDetails}
import models.{NormalMode, SelectAlternativeService, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.SelectAlternativeServicePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.PersonalDetailsValidationService
import uk.gov.hmrc.domain.Nino
import views.html.SendLetterErrorView

import java.time.LocalDate
import scala.concurrent.Future

class SendLetterErrorControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  lazy val sendLetterErrorRoute = routes.SendLetterErrorController.onPageLoad(mode = NormalMode).url
  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]

  val fakePDVResponseData: PDVResponseData = PDVResponseData(
    id = "fakeId",
    validationStatus = "success",
    personalDetails = Some(PersonalDetails(
      firstName = "John",
      lastName = "Doe",
      nino = Nino("AA000003B"),
      postCode = Some("AA1 1AA"),
      dateOfBirth = LocalDate.of(1990, 1, 1)
    )),
    validCustomer = Some("true"),
    CRN = Some("fakeCRN"),
    npsPostCode = Some("AA1 1AA"),
    reason = None
  )

  val fakePDVResponseDataInvalidCustomer: PDVResponseData = fakePDVResponseData.copy(
    validCustomer = Some("false")
  )

  val formProvider = new SelectAlternativeServiceFormProvider()
  val form = formProvider()

  "SendLetterError Controller" - {

    "must return OK and the correct view for a GET" in {
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseData)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, sendLetterErrorRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SendLetterErrorView]

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces mustEqual view(form, NormalMode)(request, messages, config).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseData)))

      val userAnswers = UserAnswers(userAnswersId).set(SelectAlternativeServicePage, SelectAlternativeService.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, sendLetterErrorRoute)

        val view = application.injector.instanceOf[SendLetterErrorView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces mustEqual view(form.fill(SelectAlternativeService.values.head), NormalMode)(request, messages, config).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseData)))

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, sendLetterErrorRoute)
            .withFormUrlEncodedBody(("value", SelectAlternativeService.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseData)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, sendLetterErrorRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[SendLetterErrorView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result).removeAllNonces mustEqual view(boundForm, NormalMode)(request, messages, config).toString
      }
    }

    "must redirect to unauthorised controller when the user is not a valid customer" in {
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseDataInvalidCustomer)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, sendLetterErrorRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must redirect to journey recovery controller when there is no PDV data" in {
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(None))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, sendLetterErrorRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to logged out controller when there is no cached data" in {
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(None))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, sendLetterErrorRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual auth.routes.AuthController.signout(None, None).url
      }
    }
  }
}
