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
import cacheables.TryAgainCountCacheable
import forms.LetterTechnicalErrorFormProvider
import models.pdv.{PDVResponseData, PersonalDetails, ValidationStatus}
import models.{LetterTechnicalError, NormalMode, UserAnswers}
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
import repositories.SessionRepository
import services.PersonalDetailsValidationService
import uk.gov.hmrc.domain.Nino
import views.html.LetterTechnicalErrorView

import java.time.LocalDate
import scala.concurrent.Future

class LetterTechnicalErrorControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val getNINOByPostUrl = "http://localhost:11300/fill-online/get-your-national-insurance-number-by-post"

  lazy val letterTechnicalErrorRoute: String = routes.LetterTechnicalErrorController.onPageLoad().url

  val formProvider                                                           = new LetterTechnicalErrorFormProvider()
  val form: Form[LetterTechnicalError]                                       = formProvider()
  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val fakePDVResponseDataWithPostcode: PDVResponseData = PDVResponseData(
    id = "fakeId",
    validationStatus = ValidationStatus.Success,
    personalDetails = Some(
      PersonalDetails(
        firstName = "John",
        lastName = "Doe",
        nino = Nino("AB123456C"),
        postCode = Some("AA1 1AA"),
        dateOfBirth = LocalDate.of(1990, 1, 1)
      )
    ),
    validCustomer = Some(true),
    CRN = Some("fakeCRN"),
    npsPostCode = Some("AA1 1AA"),
    reason = None
  )

  val fakePDVResponseDataWithoutPostcode: PDVResponseData = PDVResponseData(
    id = "fakeId",
    validationStatus = ValidationStatus.Success,
    personalDetails = Some(
      PersonalDetails(
        firstName = "John",
        lastName = "Doe",
        nino = Nino("AB123456C"),
        postCode = None,
        dateOfBirth = LocalDate.of(1990, 1, 1)
      )
    ),
    validCustomer = Some(true),
    CRN = Some("fakeCRN"),
    npsPostCode = Some("AA1 1AA"),
    reason = None
  )

  val fakePDVResponseDataInvalidCustomer: PDVResponseData = fakePDVResponseDataWithPostcode.copy(
    validCustomer = Some(false)
  )

  "LetterTechnicalErrorController" - {

    "must return OK and the correct view for a GET" in {

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseDataWithPostcode)))

      val application =
        applicationBuilder(userAnswers = emptyUserAnswers.set(TryAgainCountCacheable, 0).success.value)
          .overrides(
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, letterTechnicalErrorRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LetterTechnicalErrorView]

        status(result) mustEqual OK
        contentAsString(result)
          .removeAllNonces() mustEqual view(form, NormalMode, retryAllowed = true)(request, messages).toString
      }
    }

    "must set retry allowed to false when count is greater than 5" in {

      val tryAgainCount = 7

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseDataWithPostcode)))

      val application =
        applicationBuilder(userAnswers = emptyUserAnswers.set(TryAgainCountCacheable, tryAgainCount).success.value)
          .overrides(
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, letterTechnicalErrorRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[LetterTechnicalErrorView]

        status(result) mustEqual OK
        contentAsString(result)
          .removeAllNonces() mustEqual view(form, NormalMode, retryAllowed = false)(request, messages).toString
      }
    }

    "must not populate any value in the view on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers()
        .set(LetterTechnicalErrorPage, LetterTechnicalError.values.head)
        .success
        .value
        .set(TryAgainCountCacheable, 0)
        .success
        .value

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseDataWithPostcode)))
      val application = applicationBuilder(userAnswers = userAnswers)
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, letterTechnicalErrorRoute)

        val view = application.injector.instanceOf[LetterTechnicalErrorView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result)
          .removeAllNonces() mustEqual view(form, NormalMode, retryAllowed = true)(request, messages).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.setUserAnswers(any(), any())) thenReturn Future.successful(true)
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseDataWithPostcode)))

      val application =
        applicationBuilder()
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

    "must return a Bad Request and errors when invalid data is submitted and retry count < 5" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.setUserAnswers(any(), any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = emptyUserAnswers.set(TryAgainCountCacheable, 0).success.value)
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, letterTechnicalErrorRoute)
            .withFormUrlEncodedBody(("value", "invalid"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "redirect to SelectNINOLetterAddress page for a POST if user selects Try again option with a postcode" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.setUserAnswers(any(), any())) thenReturn Future.successful(true)

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseDataWithPostcode)))

      val application =
        applicationBuilder(userAnswers = emptyUserAnswers.set(TryAgainCountCacheable, 0).success.value)
          .overrides(
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            bind[SessionRepository].toInstance(mockSessionRepository)
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
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.setUserAnswers(any(), any())) thenReturn Future.successful(true)
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseDataWithoutPostcode)))

      val application =
        applicationBuilder(userAnswers = emptyUserAnswers.set(TryAgainCountCacheable, 0).success.value)
          .overrides(
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            bind[SessionRepository].toInstance(mockSessionRepository)
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
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.setUserAnswers(any(), any())) thenReturn Future.successful(true)
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseDataWithPostcode)))

      val application = applicationBuilder()
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          bind[SessionRepository].toInstance(mockSessionRepository)
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
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.setUserAnswers(any(), any())) thenReturn Future.successful(true)
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseDataWithPostcode)))

      val application = applicationBuilder()
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          bind[SessionRepository].toInstance(mockSessionRepository)
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

      val application = applicationBuilder()
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, letterTechnicalErrorRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must redirect to journey recovery controller when there is no PDV data" in {
      // val userAnswers = UserAnswers().setOrException(ValidDataNINOMatchedNINOHelpPage, true)
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(None))

      val application = applicationBuilder(nonEmptyUserAnswers)
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, letterTechnicalErrorRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to logged out controller when there is no cached data" in {
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(None))

      val application = applicationBuilder()
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, letterTechnicalErrorRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual auth.routes.SignedOutController.onPageLoad.url
      }
    }
  }
}
