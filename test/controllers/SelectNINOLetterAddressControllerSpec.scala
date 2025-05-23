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
import connectors.NPSFMNConnector
import forms.SelectNINOLetterAddressFormProvider
import models.nps.{LetterIssuedResponse, RLSDLONFAResponse, TechnicalIssueResponse}
import models.pdv.{PDVResponseData, PersonalDetails, ValidationStatus}
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{ConfirmYourPostcodePage, SelectNINOLetterAddressPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.{NPSFMNService, PersonalDetailsValidationService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import views.html.SelectNINOLetterAddressView

import java.time.LocalDate
import scala.concurrent.Future

class SelectNINOLetterAddressControllerSpec extends SpecBase with MockitoSugar {

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  override val hc: HeaderCarrier                     = HeaderCarrier()

  lazy val selectNINOLetterAddressRoute: String = routes.SelectNINOLetterAddressController.onPageLoad(NormalMode).url

  val formProvider = new SelectNINOLetterAddressFormProvider()
  val form         = formProvider()

  val fakePDVResponseData: PDVResponseData = PDVResponseData(
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
    npsPostCode = None,
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

  val fakePDVResponseDataWithoutPostcodeNoMatch: PDVResponseData =
    fakePDVResponseDataWithoutPostcode.copy(npsPostCode = Some("AA2 2BB"))

  val fakePDVResponseDataInvalidCustomer: PDVResponseData = fakePDVResponseData.copy(
    validCustomer = Some(false)
  )

  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]

  "SelectNINOLetterAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseData)))

      val application = applicationBuilder()
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, selectNINOLetterAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SelectNINOLetterAddressView]

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces() mustEqual view(
          form,
          NormalMode,
          fakePDVResponseData.personalDetails.get.postCode.get
        )(request, messages).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseData)))

      val userAnswers = UserAnswers().set(SelectNINOLetterAddressPage, true).success.value

      val application = applicationBuilder(userAnswers = userAnswers)
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, selectNINOLetterAddressRoute)

        val view = application.injector.instanceOf[SelectNINOLetterAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces() mustEqual view(
          form.fill(true),
          NormalMode,
          fakePDVResponseData.personalDetails.get.postCode.get
        )(request, messages).toString
      }
    }

    "must redirect to the confirmation page and call NPS FMN letter API when yes option is selected with valid data" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector   = mock[NPSFMNConnector]
      val mockNPSFMNService     = mock[NPSFMNService]

      when(mockSessionRepository.setUserAnswers(any(), any())) thenReturn Future.successful(true)
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(LetterIssuedResponse()))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseData)))

      val application =
        applicationBuilder()
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, selectNINOLetterAddressRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NINOLetterPostedConfirmationController.onPageLoad().url
        verify(mockNPSFMNService, times(1)).sendLetter(any(), any())(any(), any())
      }
    }

    "must redirect to service alternatives and not call NPS FMN letter API when no option is selected" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector   = mock[NPSFMNConnector]
      val mockNPSFMNService     = mock[NPSFMNService]

      when(mockSessionRepository.setUserAnswers(any(), any())) thenReturn Future.successful(true)
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(LetterIssuedResponse()))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseData)))

      val application =
        applicationBuilder()
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, selectNINOLetterAddressRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SelectAlternativeServiceController.onPageLoad().url
        verify(mockNPSFMNService, never()).sendLetter(any(), any())(any(), any())
      }
    }

    "must redirect to the send letter error page when invalid data is submitted to NPS FMN API" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector   = mock[NPSFMNConnector]
      val mockNPSFMNService     = mock[NPSFMNService]

      when(mockSessionRepository.setUserAnswers(any(), any())) thenReturn Future.successful(true)
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(RLSDLONFAResponse(SEE_OTHER, "some message")))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseData)))

      val application =
        applicationBuilder()
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, selectNINOLetterAddressRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SendLetterErrorController.onPageLoad(NormalMode).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseData)))

      val application = applicationBuilder()
        .overrides(bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, selectNINOLetterAddressRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[SelectNINOLetterAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result).removeAllNonces() mustEqual view(
          boundForm,
          NormalMode,
          fakePDVResponseData.personalDetails.get.postCode.get
        )(request, messages).toString
      }
    }

    "redirect to Technical error page for a POST when NPS FMN API returns error status other than 202 and 400" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector   = mock[NPSFMNConnector]
      val mockNPSFMNService     = mock[NPSFMNService]

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseData)))

      val application =
        applicationBuilder()
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
          )
          .build()

      when(mockSessionRepository.setUserAnswers(any(), any())) thenReturn Future.successful(true)
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(TechnicalIssueResponse(SEE_OTHER, "some message")))

      running(application) {
        val request =
          FakeRequest(POST, selectNINOLetterAddressRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.LetterTechnicalErrorController.onPageLoad().url
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
        val request = FakeRequest(GET, selectNINOLetterAddressRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must redirect to journey recovery controller when there is no PDV data" in {
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(None))

      val application = applicationBuilder(nonEmptyUserAnswers)
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, selectNINOLetterAddressRoute)
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
        val request = FakeRequest(GET, selectNINOLetterAddressRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual auth.routes.SignedOutController.onPageLoad.url
      }
    }

    "must return OK and the correct view for a GET when user has come from confirm-your-postcode" in {
      val userAnswers: UserAnswers = UserAnswers().set(ConfirmYourPostcodePage, "AA1 1AA").success.value

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseDataWithoutPostcode)))

      val application = applicationBuilder(userAnswers = userAnswers)
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, selectNINOLetterAddressRoute)
        val result  = route(application, request).value

        val view = application.injector.instanceOf[SelectNINOLetterAddressView]
        status(result) mustEqual OK
        contentAsString(result).removeAllNonces() mustEqual view(
          form,
          NormalMode,
          fakePDVResponseData.personalDetails.get.postCode.get
        )(request, messages).toString
      }
    }

    "must return OK and the correct view for a GET when user has come from confirm-your-postcode no match" in {
      val userAnswers: UserAnswers = UserAnswers().set(ConfirmYourPostcodePage, "AA1 1AA").success.value

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseDataWithoutPostcodeNoMatch)))

      val application = applicationBuilder(userAnswers = userAnswers)
        .overrides(
          bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, selectNINOLetterAddressRoute)

        intercept[IllegalArgumentException] {
          await(route(application, request).value)
        }
      }
    }
  }
}
