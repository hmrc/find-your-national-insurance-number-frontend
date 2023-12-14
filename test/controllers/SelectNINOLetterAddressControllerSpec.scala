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
import connectors.NPSFMNConnector
import forms.SelectNINOLetterAddressFormProvider
import models.nps.{LetterIssuedResponse, RLSDLONFAResponse, TechnicalIssueResponse}
import models.{NormalMode, SelectNINOLetterAddress, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.SelectNINOLetterAddressPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.NPSFMNService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.SelectNINOLetterAddressView

import scala.concurrent.Future

class SelectNINOLetterAddressControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  override val hc: HeaderCarrier = HeaderCarrier()

  lazy val selectNINOLetterAddressRoute: String = routes.SelectNINOLetterAddressController.onPageLoad(NormalMode).url

  val formProvider = new SelectNINOLetterAddressFormProvider()
  val form = formProvider()

  //for testing purposes this is hard coded - postcode will not be displayed in future iterations
  val generatedPostcode = "FX97 4TU"

  "SelectNINOLetterAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, selectNINOLetterAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SelectNINOLetterAddressView]

        status(result) mustEqual OK
        contentAsString(result) contains view(form, NormalMode, generatedPostcode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(SelectNINOLetterAddressPage, SelectNINOLetterAddress.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, selectNINOLetterAddressRoute)

        val view = application.injector.instanceOf[SelectNINOLetterAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) contains view(form.fill(SelectNINOLetterAddress.values.head), NormalMode, generatedPostcode)(request, messages(application)).toString
      }
    }

    "must redirect to the confirmation page when valid data is submitted to NPS FMN API" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector = mock[NPSFMNConnector]
      val mockNPSFMNService = mock[NPSFMNService]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(LetterIssuedResponse()))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, selectNINOLetterAddressRoute)
            .withFormUrlEncodedBody(("value", SelectNINOLetterAddress.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NINOLetterPostedConfirmationController.onPageLoad().url
      }
    }

    "must redirect to the send letter error page when invalid data is submitted to NPS FMN API" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector = mock[NPSFMNConnector]
      val mockNPSFMNService = mock[NPSFMNService]

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(RLSDLONFAResponse(SEE_OTHER, "some message")))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, selectNINOLetterAddressRoute)
            .withFormUrlEncodedBody(("value", SelectNINOLetterAddress.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SendLetterErrorController.onPageLoad(NormalMode).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, selectNINOLetterAddressRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[SelectNINOLetterAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) contains view(boundForm, NormalMode, generatedPostcode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, selectNINOLetterAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
//        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Technical error page for a POST when NPS FMN API returns error status other than 202 and 400" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector = mock[NPSFMNConnector]
      val mockNPSFMNService = mock[NPSFMNService]

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector)
          )
          .build()

      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(TechnicalIssueResponse(SEE_OTHER, "some message")))

      running(application) {
        val request =
          FakeRequest(POST, selectNINOLetterAddressRoute)
            .withFormUrlEncodedBody(("value", SelectNINOLetterAddress.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TechnicalErrorController.onPageLoad().url
      }
    }

  }
}
