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
import connectors.{IndividualDetailsConnector, NPSFMNConnector}
import forms.ConfirmYourPostcodeFormProvider
import models.individualdetails._
import models.nps.{LetterIssuedResponse, RLSDLONFAResponse, TechnicalIssueResponse}
import models.pdv.{PDVResponseData, PersonalDetails}
import models.{AddressLine, CorrelationId, IndividualDetailsResponseEnvelope, NormalMode, UserAnswers, individualdetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import pages.ConfirmYourPostcodePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.{NPSFMNService, PersonalDetailsValidationService}
import uk.gov.hmrc.domain.Nino
import views.html.ConfirmYourPostcodeView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import util.AnyValueTypeMatcher.anyValueType

class ConfirmYourPostcodeControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  implicit val correlationId: models.CorrelationId = models.CorrelationId.random
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val formProvider = new ConfirmYourPostcodeFormProvider()
  val form = formProvider()

  lazy val confirmYourPostcodeRoute = routes.ConfirmYourPostcodeController.onPageLoad(NormalMode).url
  val controller: ConfirmYourPostcodeController = application.injector.instanceOf[ConfirmYourPostcodeController]

  val fakePDVResponseData: PDVResponseData = PDVResponseData(
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
    npsPostCode = Some("AA1 1  AA"),
    reason = None
  )

  val fakePDVResponseDataFailure: PDVResponseData = PDVResponseData(
    id = "fakeId",
    validationStatus = "failure",
    personalDetails = None,
    validCustomer = Some("true"),
    CRN = Some("fakeCRN"),
    npsPostCode = Some("AA1 1  AA"),
    reason = None
  )

  val fakeName: individualdetails.Name = models.individualdetails.Name(
    nameSequenceNumber = NameSequenceNumber(1),
    nameType = NameType.RealName,
    titleType = Some(TitleType.Mr),
    requestedName = Some(RequestedName("John Doe")),
    nameStartDate = NameStartDate(LocalDate.of(2000, 1, 1)),
    nameEndDate = Some(NameEndDate(LocalDate.of(2022, 12, 31))),
    otherTitle = Some(OtherTitle("Sir")),
    honours = Some(Honours("PhD")),
    firstForename = FirstForename("John"),
    secondForename = Some(SecondForename("Doe")),
    surname = Surname("Smith")
  )

  val fakeAddress: Address = Address(
    addressSequenceNumber = AddressSequenceNumber(0),
    addressSource = Some(AddressSource.Customer),
    countryCode = CountryCode(826), // 826 is the numeric code for the United Kingdom
    addressType = AddressType.ResidentialAddress,
    addressStatus = Some(AddressStatus.NotDlo),
    addressStartDate = LocalDate.of(2000, 1, 1),
    addressEndDate = Some(LocalDate.of(2022, 12, 31)),
    addressLastConfirmedDate = Some(LocalDate.of(2022, 1, 1)),
    vpaMail = Some(VpaMail(1)),
    deliveryInfo = Some(DeliveryInfo("Delivery info")),
    pafReference = Some(PafReference("PAF reference")),
    addressLine1 = AddressLine("123 Fake Street"),
    addressLine2 = AddressLine("Apt 4B"),
    addressLine3 = Some(AddressLine("Faketown")),
    addressLine4 = Some(AddressLine("Fakeshire")),
    addressLine5 = Some(AddressLine("Fakecountry")),
    addressPostcode = Some(AddressPostcode("AA1 1AA"))
  )

  val fakeIndividualDetails: IndividualDetails = IndividualDetails(
    ninoWithoutSuffix = "AB123456",
    ninoSuffix = Some(NinoSuffix("C")),
    accountStatusType = Some(AccountStatusType.FullLive),
    dateOfEntry = Some(LocalDate.of(2000, 1, 1)),
    dateOfBirth = LocalDate.of(1990, 1, 1),
    dateOfBirthStatus = Some(DateOfBirthStatus.Verified),
    dateOfDeath = None,
    dateOfDeathStatus = None,
    dateOfRegistration = Some(LocalDate.of(2000, 1, 1)),
    crnIndicator = CrnIndicator.False,
    nameList = NameList(Some(List(fakeName))),
    addressList = AddressList(Some(List(fakeAddress)))

  )

  "ConfirmYourPostcode Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, confirmYourPostcodeRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmYourPostcodeView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the confirmation page when valid data is submitted to NPS FMN API" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector = mock[NPSFMNConnector]
      val mockNPSFMNService = mock[NPSFMNService]
      val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
      val mockIndividualDetailsConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseData)))
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(LetterIssuedResponse()))


      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector),
            bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, confirmYourPostcodeRoute)
            .withFormUrlEncodedBody(("value", "AA1 1AA"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NINOLetterPostedConfirmationController.onPageLoad().url
      }
    }

    "must redirect to postcode issue page when postcode entered doesn't match NPS postcode" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector = mock[NPSFMNConnector]
      val mockNPSFMNService = mock[NPSFMNService]
      val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
      val mockIndividualDetailsConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseData)))
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(LetterIssuedResponse()))


      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector),
            bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, confirmYourPostcodeRoute)
            .withFormUrlEncodedBody(("value", "AA2 1AA"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.EnteredPostCodeNotFoundController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to Technical error page when PDV details missing" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector = mock[NPSFMNConnector]
      val mockNPSFMNService = mock[NPSFMNService]
      val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
      val mockIndividualDetailsConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseDataFailure)))
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(LetterIssuedResponse()))


      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector),
            bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, confirmYourPostcodeRoute)
            .withFormUrlEncodedBody(("value", "AA1 1AA"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TechnicalErrorController.onPageLoad().url
      }
    }

    "must redirect to the send letter error page when invalid data is submitted to NPS FMN API" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector = mock[NPSFMNConnector]
      val mockNPSFMNService = mock[NPSFMNService]
      val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
      val mockIndividualDetailsConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseData)))
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(RLSDLONFAResponse(SEE_OTHER, "some message")))


      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector),
            bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, confirmYourPostcodeRoute)
            .withFormUrlEncodedBody(("value", "AA1 1AA"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SendLetterErrorController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to Technical error page for a POST when NPS FMN API returns error status other than 202 and 400" in {
      val mockSessionRepository = mock[SessionRepository]
      val mockNPSFMNConnector = mock[NPSFMNConnector]
      val mockNPSFMNService = mock[NPSFMNService]
      val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
      val mockIndividualDetailsConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any()))
        .thenReturn(Future(Some(fakePDVResponseData)))
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(TechnicalIssueResponse(SEE_OTHER, "some message")))


      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector),
            bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, confirmYourPostcodeRoute)
            .withFormUrlEncodedBody(("value", "AA1 1AA"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TechnicalErrorController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId).set(ConfirmYourPostcodePage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, confirmYourPostcodeRoute)

        val view = application.injector.instanceOf[ConfirmYourPostcodeView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, confirmYourPostcodeRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ConfirmYourPostcodeView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

  }
}
