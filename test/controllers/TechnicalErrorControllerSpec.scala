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
import forms.TechnicalErrorServiceFormProvider
import models.pdv.{PDVResponseData, PersonalDetails}
import models.{NormalMode, TechnicalErrorService, TryAgainCount, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.TechnicalErrorPage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.{SessionRepository, TryAgainCountRepository}
import services.PersonalDetailsValidationService
import uk.gov.hmrc.domain.Nino
import views.html.TechnicalErrorView

import java.time.LocalDate
import scala.concurrent.Future

class TechnicalErrorControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call = Call("GET", "/foo")

  val getNINOByPostUrl = "http://localhost:11300/fill-online/get-your-national-insurance-number-by-post"

  lazy val technicalErrorRoute: String = routes.TechnicalErrorController.onPageLoad().url

  val formProvider = new TechnicalErrorServiceFormProvider()
  val form: Form[TechnicalErrorService] = formProvider()

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

  "TechnicalErrorController" - {

    "must return OK and the correct view for a GET" in {
      val mockTryAgainCountRepository = mock[TryAgainCountRepository]
      when(mockTryAgainCountRepository.findById(any())(any())) thenReturn Future.successful(Some(TryAgainCount(id = "", count = 0)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[TryAgainCountRepository].toInstance(mockTryAgainCountRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, technicalErrorRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TechnicalErrorView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, true)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(TechnicalErrorPage, TechnicalErrorService.values.head).success.value
      val mockTryAgainCountRepository = mock[TryAgainCountRepository]

      when(mockTryAgainCountRepository.findById(any())(any())) thenReturn Future.successful(Some(TryAgainCount(id = "", count = 0)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[TryAgainCountRepository].toInstance(mockTryAgainCountRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, technicalErrorRoute)

        val view = application.injector.instanceOf[TechnicalErrorView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(TechnicalErrorService.values.head), NormalMode, true)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, technicalErrorRoute)
            .withFormUrlEncodedBody(("value", TechnicalErrorService.PhoneHmrc.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "redirect to SelectNINOLetterAddress page for a POST if user selects Try again option with a postcode" in {

      val mockTryAgainCountRepository = mock[TryAgainCountRepository]
      val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]

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
          FakeRequest(POST, technicalErrorRoute)
            .withFormUrlEncodedBody(("value", TechnicalErrorService.TryAgain.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.SelectNINOLetterAddressController.onPageLoad(NormalMode).url
      }
    }

    "redirect to ConfirmYourPostcode page for a POST if user selects Try again option without a postcode" in {

      val mockTryAgainCountRepository = mock[TryAgainCountRepository]
      val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]

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
          FakeRequest(POST, technicalErrorRoute)
            .withFormUrlEncodedBody(("value", TechnicalErrorService.TryAgain.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.ConfirmYourPostcodeController.onPageLoad(NormalMode).url
      }
    }

    "redirect to PhoneHMRCDetails page for a POST if user selects Phone HMRC option" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, technicalErrorRoute)
            .withFormUrlEncodedBody(("value", TechnicalErrorService.PhoneHmrc.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.PhoneHMRCDetailsController.onPageLoad().url
      }
    }

    "redirect to Print and Post page for a POST if user selects P&P Service option" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, technicalErrorRoute)
            .withFormUrlEncodedBody(("value", TechnicalErrorService.PrintForm.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual getNINOByPostUrl
      }
    }
  }
}
