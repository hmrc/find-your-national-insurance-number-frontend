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

package controllers

import base.SpecBase
import forms.{NoRetryTechnicalErrorServiceFormProvider, TechnicalErrorServiceFormProvider}
import models.{NormalMode, TechnicalErrorService, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.TechnicalErrorPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.TechnicalErrorView

import scala.concurrent.Future

class TechnicalErrorControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val getNINOByPostUrl = "http://localhost:11300/fill-online/get-your-national-insurance-number-by-post"

  lazy val technicalErrorRoute = routes.TechnicalErrorController.onPageLoad().url

  val formProvider = new TechnicalErrorServiceFormProvider()
  val form = formProvider()

  "TechnicalErrorController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, technicalErrorRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TechnicalErrorView]

        status(result) mustEqual OK
        //contentAsString(result) mustEqual view(form, NormalMode, true)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(TechnicalErrorPage, TechnicalErrorService.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, technicalErrorRoute)

        val view = application.injector.instanceOf[TechnicalErrorView]

        val result = route(application, request).value

        status(result) mustEqual OK
        //contentAsString(result) mustEqual view(form.fill(TechnicalErrorService.values.head), NormalMode, true)(request, messages(application)).toString
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
            .withFormUrlEncodedBody(("value", TechnicalErrorService.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, technicalErrorRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[TechnicalErrorView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        //contentAsString(result) mustEqual view(boundForm, NormalMode, true)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, technicalErrorRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "redirect to electNINOLetterAddress page for a POST if user selects Try again option" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, technicalErrorRoute)
            .withFormUrlEncodedBody(("value", TechnicalErrorService.TryAgain.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.SelectNINOLetterAddressController.onPageLoad(NormalMode).url
      }
    }

    "redirect to electNINOLetterAddress page for a POST if user selects Phone HMRC option" in {

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

    "redirect to electNINOLetterAddress page for a POST if user selects P&P Service option" in {

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
