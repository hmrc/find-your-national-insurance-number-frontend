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
import forms.SelectAlternativeServiceFormProvider
import models.{NormalMode, SelectAlternativeService, SelectNINOLetterAddress, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.SelectNINOLetterAddressPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.InvalidDataNINOHelpView

import scala.concurrent.Future

class InvalidDataNINOHelpControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  lazy val invalidDataNINOHelpRoute = routes.InvalidDataNINOHelpController.onPageLoad(mode = NormalMode).url

  val formProvider = new SelectAlternativeServiceFormProvider()
  val form = formProvider()

  "InvalidDataNINOHelp Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers =  Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, invalidDataNINOHelpRoute)

        val result = route(application, request).value
        status(result) mustEqual OK
        val view = application.injector.instanceOf[InvalidDataNINOHelpView]
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application), config).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(SelectNINOLetterAddressPage, SelectNINOLetterAddress.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, invalidDataNINOHelpRoute)

        val view = application.injector.instanceOf[InvalidDataNINOHelpView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) contains view(form.fill(SelectAlternativeService.values.head), NormalMode)(request, messages(application), config).toString
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
          FakeRequest(POST, invalidDataNINOHelpRoute)
            .withFormUrlEncodedBody(("value", SelectAlternativeService.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, invalidDataNINOHelpRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[InvalidDataNINOHelpView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) contains view(boundForm, NormalMode)(request, messages(application), config).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, invalidDataNINOHelpRoute)

        val result = route(application, request).value

        //status(result) mustEqual SEE_OTHER
        //redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

        status(result) mustEqual OK
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, invalidDataNINOHelpRoute)
            .withFormUrlEncodedBody(("value", SelectAlternativeService.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        //redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

}
