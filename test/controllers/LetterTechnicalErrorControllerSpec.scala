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
import forms.LetterTechnicalErrorFormProvider
import models.pdv.{PDVResponseData, PersonalDetails}
import models.{NormalMode, LetterTechnicalError, TryAgainCount, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.LetterTechnicalErrorPage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.{SessionRepository, TryAgainCountRepository}
import services.PersonalDetailsValidationService
import uk.gov.hmrc.domain.Nino
import views.html.LetterTechnicalErrorView

import java.time.LocalDate
import scala.concurrent.Future

class LetterTechnicalErrorControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val getNINOByPostUrl = "http://localhost:11300/fill-online/get-your-national-insurance-number-by-post"

  lazy val letterTechnicalErrorRoute: String = routes.LetterTechnicalErrorController.onPageLoad().url

  val formProvider = new LetterTechnicalErrorFormProvider()
  val form: Form[LetterTechnicalError] = formProvider()
  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val fakePDVResponseDataWithPostcode: PDVResponseData = PDVResponseData(
    id = "fakeId",
    validationStatus = "success",
    personalDetails = Some(PersonalDetails(
      firstName = "John",
      lastName = "Doe",
      nino = Nino("AB123456C"),
      postCode = Some("AA1 1AA"),
      dateOfBirth = LocalDate.of(1990, 1, 1)
    )),
    validCustomer = Some("true"),
    CRN = Some("fakeCRN"),
    npsPostCode = Some("AA1 1AA"),
    reason = None
  )

  val fakePDVResponseDataWithoutPostcode: PDVResponseData = PDVResponseData(
    id = "fakeId",
    validationStatus = "success",
    personalDetails = Some(PersonalDetails(
      firstName = "John",
      lastName = "Doe",
      nino = Nino("AB123456C"),
      postCode = None,
      dateOfBirth = LocalDate.of(1990, 1, 1)
    )),
    validCustomer = Some("true"),
    CRN = Some("fakeCRN"),
    npsPostCode = Some("AA1 1AA"),
    reason = None
  )

  val fakePDVResponseDataInvalidCustomer: PDVResponseData = fakePDVResponseDataWithPostcode.copy(
    validCustomer = Some("false")
  )

  "LetterTechnicalErrorController" - {

    "must return OK and the correct view for a GET" in {
      val mockTryAgainCountRepository = mock[TryAgainCountRepository]
      when(mockTryAgainCountRepository.findById(any())(any())) thenReturn Future.successful(Some(TryAgainCount(id = "", count = 0)))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseDataWithPostcode)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TryAgainCountRepository].toInstance(mockTryAgainCountRepository),
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, letterTechnicalErrorRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LetterTechnicalErrorView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, true)(request, messages).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(LetterTechnicalErrorPage, LetterTechnicalError.values.head).success.value
      val mockTryAgainCountRepository = mock[TryAgainCountRepository]

      when(mockTryAgainCountRepository.findById(any())(any())) thenReturn Future.successful(Some(TryAgainCount(id = "", count = 0)))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseDataWithPostcode)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[TryAgainCountRepository].toInstance(mockTryAgainCountRepository),
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, letterTechnicalErrorRoute)

        val view = application.injector.instanceOf[LetterTechnicalErrorView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(LetterTechnicalError.values.head), NormalMode, true)(request, messages).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseDataWithPostcode)))

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
          FakeRequest(POST, letterTechnicalErrorRoute)
            .withFormUrlEncodedBody(("value", LetterTechnicalError.PhoneHmrc.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "redirect to SelectNINOLetterAddress page for a POST if user selects Try again option with a postcode" in {

      val mockTryAgainCountRepository = mock[TryAgainCountRepository]

      when(mockTryAgainCountRepository.findById(any())(any())) thenReturn Future.successful(Some(TryAgainCount(id = "", count = 0)))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseDataWithPostcode)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TryAgainCountRepository].toInstance(mockTryAgainCountRepository),
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, letterTechnicalErrorRoute)
            .withFormUrlEncodedBody(("value", LetterTechnicalError.TryAgain.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.SelectNINOLetterAddressController.onPageLoad(NormalMode).url
      }
    }

    "redirect to ConfirmYourPostcode page for a POST if user selects Try again option without a postcode" in {

      val mockTryAgainCountRepository = mock[TryAgainCountRepository]

      when(mockTryAgainCountRepository.findById(any())(any())) thenReturn Future.successful(Some(TryAgainCount(id = "", count = 0)))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseDataWithoutPostcode)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TryAgainCountRepository].toInstance(mockTryAgainCountRepository),
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, letterTechnicalErrorRoute)
            .withFormUrlEncodedBody(("value", LetterTechnicalError.TryAgain.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.ConfirmYourPostcodeController.onPageLoad(NormalMode).url
      }
    }

    "redirect to PhoneHMRCDetails page for a POST if user selects Phone HMRC option" in {
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseDataWithPostcode)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, letterTechnicalErrorRoute)
            .withFormUrlEncodedBody(("value", LetterTechnicalError.PhoneHmrc.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.PhoneHMRCDetailsController.onPageLoad().url
      }
    }

    "redirect to Print and Post page for a POST if user selects P&P Service option" in {
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseDataWithPostcode)))

      val application = applicationBuilder(userAnswers = None)
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, letterTechnicalErrorRoute)
            .withFormUrlEncodedBody(("value", LetterTechnicalError.PrintForm.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual getNINOByPostUrl
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
        val request = FakeRequest(GET, letterTechnicalErrorRoute)
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
        val request = FakeRequest(GET, letterTechnicalErrorRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
