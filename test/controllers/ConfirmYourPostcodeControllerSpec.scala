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
import config.{DesApiServiceConfig, FrontendAppConfig}
import connectors.{DefaultIndividualDetailsConnector, IndividualDetailsConnector, NPSFMNConnector}
import forms.ConfirmYourPostcodeFormProvider
import models.individualdetails._
import models.nps.LetterIssuedResponse
import models.pdv.{PDVResponseData, PersonalDetails}
import models.{AddressLine, CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope, NormalMode, UserAnswers, individualdetails}
import org.mockito.ArgumentMatchers.{any, anyChar, argThat}
import org.mockito.MockitoSugar.when
import org.scalatestplus.mockito.MockitoSugar
import pages.ConfirmYourPostcodePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.{NPSFMNService, PersonalDetailsValidationService}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, CredentialRole, User}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import views.html.ConfirmYourPostcodeView
import com.kenshoo.play.metrics.Metrics

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import org.mockito.ArgumentMatcher
import java.util.UUID
import repositories.PersonalDetailsValidationRepository
import repositories.TryAgainCountRepository

class ConfirmYourPostcodeControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  implicit val correlationId: models.CorrelationId = models.CorrelationId.random
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val formProvider = new ConfirmYourPostcodeFormProvider()
  val form = formProvider()

  lazy val confirmYourPostcodeRoute = routes.ConfirmYourPostcodeController.onPageLoad(NormalMode).url
  val controller: ConfirmYourPostcodeController = application.injector.instanceOf[ConfirmYourPostcodeController]



//  when(mockDesApiServiceConfig.token).thenReturn("test")
//  when(mockDesApiServiceConfig.environment).thenReturn("test")
//  when(mockDesApiServiceConfig.originatorId).thenReturn("test")


  val retrievalResult: Future[Option[String] ~ Option[CredentialRole] ~ Option[String]] =
    Future.successful(new~(new~(Some("nino"), Some(User)), Some("id")))

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
    npsPostCode = Some("AA1 1AA"),
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
      val mpdvr = mock[PersonalDetailsValidationRepository]
      val mtacr = mock[TryAgainCountRepository]
      val mockNPSFMNConnector = mock[NPSFMNConnector]
      val mockNPSFMNService = mock[NPSFMNService]
      val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
      val mockHttpClient = mock[HttpClient]
      val mockAppConfig = mock[FrontendAppConfig]
      //val mockDesApiServiceConfig = mock[DesApiServiceConfig]
      val mockAuthConnector = mock[AuthConnector]
      val mockMetrics = mock[Metrics]
     val mockDefaultIndividualDetailsConnector = new DefaultIndividualDetailsConnector(mockHttpClient, mockAppConfig, mockMetrics)
      val mockIndividualDetailsConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
      when(
        mockAuthConnector.authorise[Option[String] ~ Option[CredentialRole] ~ Option[String]](
          any[Predicate],
          any[Retrieval[Option[String] ~ Option[CredentialRole] ~ Option[String]]])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(retrievalResult)
      when(mockHttpClient.GET[HttpResponse](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))
      when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any())).thenReturn(Future(Some(fakePDVResponseData)))


      trait Default[T] {
        def getDefault: T
      }

      def anyValueType[T](implicit d: Default[T]): T = {
          argThat(new ArgumentMatcher[T] {
          def matches(argument: T): Boolean = true
        })
         d.getDefault
      }

      implicit val defaultResolveMerge: Default[ResolveMerge] = new Default[ResolveMerge] { val getDefault = ResolveMerge('X') }
      implicit val defaultCorelationId: Default[CorrelationId] = new Default[CorrelationId] { val getDefault = CorrelationId(UUID.nameUUIDFromBytes(new Array[Byte](16))) }

      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

      when(mockMetrics.defaultRegistry).thenReturn(new com.codahale.metrics.MetricRegistry())
      when(mockNPSFMNService.sendLetter(any(), any())(any(), any()))
        .thenReturn(Future.successful(LetterIssuedResponse()))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[PersonalDetailsValidationRepository].toInstance(mpdvr),
            bind[TryAgainCountRepository].toInstance(mtacr),
            bind[NPSFMNService].toInstance(mockNPSFMNService),
            bind[NPSFMNConnector].toInstance(mockNPSFMNConnector),
            bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            bind[HttpClient].toInstance(mockHttpClient),
            bind[FrontendAppConfig].toInstance(mockAppConfig),
            //bind[DesApiServiceConfig].toInstance(mockDesApiServiceConfig),
            bind[Metrics].toInstance(mockMetrics),
            bind[AuthConnector].toInstance(mockAuthConnector)
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
