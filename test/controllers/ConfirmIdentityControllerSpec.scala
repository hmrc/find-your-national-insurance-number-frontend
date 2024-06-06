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
import forms.ConfirmIdentityFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.ConfirmIdentityPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.ConfirmIdentityView

import scala.concurrent.Future

class ConfirmIdentityControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new ConfirmIdentityFormProvider()
  val form = formProvider()

  lazy val confirmIdentityRoute = routes.ConfirmIdentityController.onPageLoad(NormalMode).url

  "ConfirmIdentity Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilderCl50OnWithConfig(
        Map("features.extendedIvJourney" -> false),
        userAnswers = Some(emptyUserAnswers)
      ).build()

      running(application) {
        val request = FakeRequest(GET, confirmIdentityRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmIdentityView]

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces mustEqual view(form, NormalMode)(request, messages).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(ConfirmIdentityPage, true).success.value

      val application = applicationBuilderCl50OnWithConfig(
        Map("features.extendedIvJourney" -> false),
        userAnswers = Some(userAnswers)
      ).build()

      running(application) {
        val request = FakeRequest(GET, confirmIdentityRoute)

        val view = application.injector.instanceOf[ConfirmIdentityView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces mustEqual view(form.fill(true), NormalMode)(request, messages).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilderCl50On(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, confirmIdentityRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilderCl50On(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, confirmIdentityRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ConfirmIdentityView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result).removeAllNonces mustEqual view(boundForm, NormalMode)(request, messages).toString
      }
    }

    "CL50 feature toggled off" - {
      "must redirect to store" in {

        val application = applicationBuilderCl50Off(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, confirmIdentityRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual controllers.auth.routes.AuthController.redirectToSMN.url
        }
      }
    }

    "Extended IV journey toggled on" - {
      "must redirect to the Service IV Page" in {

        val application = applicationBuilderCl50OnWithConfig(
          Map("features.extendedIvJourney" -> true),
          userAnswers = Some(emptyUserAnswers)
        ).build()

        running(application) {
          val request = FakeRequest(GET, confirmIdentityRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual controllers.routes.ServiceIvController.onPageLoad().url
        }
      }
    }
  }
}
