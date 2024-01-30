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
import connectors.IndividualDetailsConnector
import models.errors.ConnectorError
import models.individualdetails._
import models.pdv.{PDVResponseData, PersonalDetails}
import models.{AddressLine, CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope, NormalMode, TemporaryReferenceNumber, individualdetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AuditService, PersonalDetailsValidationService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import util.AnyValueTypeMatcher.anyValueType
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import java.time._
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CheckDetailsControllerSpec extends SpecBase with SummaryListFluency {

  implicit val correlationId: models.CorrelationId = models.CorrelationId(UUID.randomUUID())
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val fakePersonDetails: PersonalDetails = models.pdv.PersonalDetails(
    firstName = "John",
    lastName = "Doe",
    nino = uk.gov.hmrc.domain.Nino("AB123456C"),
    postCode = Some("AA1 1AA"),
    dateOfBirth = java.time.LocalDate.of(1990, 1, 1)
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
    countryCode = CountryCode(826),
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

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockIndividualDetailsConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
  val controller: CheckDetailsController = application.injector.instanceOf[CheckDetailsController]
  val auditService: AuditService = mock[AuditService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector, mockIndividualDetailsConnector, mockPersonalDetailsValidationService)
  }

  val pdvOrigin: Option[String] = Some("PDV")
  val ivOrigin: Option[String] = Some("IV")

  "CheckDetailsController" - {

    "must return OK and the correct view for a GET" in {

      val mockPDVResponseData = mock[PDVResponseData].copy(validationStatus = "success")

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseData))

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]
        val list = SummaryListViewModel(Seq.empty)

        status(result) mustEqual SEE_OTHER
        contentAsString(result) contains view(list)(request, messages(application)).toString
      }
    }

    "must redirect to InvalidDataNINOHelpController page when PDVResponseData is empty" in {
      val mockPDVResponseData = mock[PDVResponseData].copy(validationStatus = "failure")

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseData))

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to ValidDataNINOMatchedNINOHelpController page when PDVResponseData is matched and nino is matched, postcode is missing" in {

      val mockPDVResponseData = PDVResponseData(
        "01234",
        "success",
        Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), None, LocalDate.now())),
        LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      val fakeIndividualDetailsWithConditionsMet = fakeIndividualDetails.copy(
        accountStatusType = Some(AccountStatusType.FullLive),
        crnIndicator = CrnIndicator.False,
        addressList = AddressList(Some(List(fakeAddress.copy(addressStatus = Some(AddressStatus.NotDlo)))))
      )

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseData))

      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithConditionsMet)))

      val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        ).build()

      val controller = app.injector.instanceOf[CheckDetailsController]

      running(app) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(ivOrigin, NormalMode).url)
          .withSession("sessionId" -> "", "nino" -> "AB123456C", "credentialId" -> "")
        val result = controller.onPageLoad(ivOrigin, NormalMode)(request)

        status(result) mustEqual SEE_OTHER
        verify(mockPersonalDetailsValidationService, times(1)).createPDVDataFromPDVMatch(any())(any())
        val resp = controller.onPageLoad(ivOrigin, NormalMode)(request)
        redirectLocation(resp).value mustEqual routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to InvalidDataNINOHelpController page when IndividualDetails is failed" in {

      when(mockIndividualDetailsConnector.getIndividualDetails(TemporaryReferenceNumber("fakeNino"), ResolveMerge('Y')))
        .thenReturn(IndividualDetailsResponseEnvelope(Left(ConnectorError(INTERNAL_SERVER_ERROR, "test"))))


      val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
      val result = controller.onPageLoad(pdvOrigin, NormalMode)(request)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
    }

    "must redirect to InvalidDataNINOHelpController page when getCredentialId returns None" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new Exception("test")))

      val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
      val result = controller.onPageLoad(pdvOrigin, NormalMode)(request)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
    }

    "must redirect to InvalidDataNINOHelpController page when getPDVData returns PDVResponseData with validationStatus as failure" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new Exception("test")))
      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any())).thenReturn(Future.successful(PDVResponseData("id", "failure", None, Instant.now(), None, None, None, None)))

      val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(ivOrigin, NormalMode).url)
      val result = controller.onPageLoad(ivOrigin, NormalMode)(request)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
    }

    "must redirect to InvalidDataNINOHelpController page when getIdData returns a Left with IndividualDetailsError" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new Exception("test")))
      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(PDVResponseData("id", "success", None, Instant.now(), None, None, None, None)))
      when(mockIndividualDetailsConnector.getIndividualDetails(IndividualDetailsNino("fakeNino"), ResolveMerge('Y')))
        .thenReturn(IndividualDetailsResponseEnvelope(Left(ConnectorError(INTERNAL_SERVER_ERROR, "error"))))

      val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(ivOrigin, NormalMode).url)
      val result = controller.onPageLoad(ivOrigin, NormalMode)(request)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
    }

    "must redirect to InvalidDataNINOHelpController page when PDVResponseData validationStatus is failure" in {
      val mockPDVResponseData = PDVResponseData(
        "01234",
        "failure",
        Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), None, LocalDate.now())),
        LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseData))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to ValidDataNINOMatchedNINOHelpController page when getIdData returns a Right " +
      "with IndividualDetails and checkConditions returns true" in {

      val mockPDVResponseData = PDVResponseData(
        "01234",
        "success",
        Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), None, LocalDate.now())),
        LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      val fakeIndividualDetailsWithConditionsMet = fakeIndividualDetails.copy(
        accountStatusType = Some(AccountStatusType.FullLive),
        crnIndicator = CrnIndicator.False,
        addressList = AddressList(Some(List(fakeAddress.copy(addressStatus = Some(AddressStatus.NotDlo)))))
      )

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseData))

      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithConditionsMet)))

      val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        ).build()

      val controller = app.injector.instanceOf[CheckDetailsController]

      running(app) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          .withSession("sessionId" -> "", "nino" -> "AB123456C", "credentialId" -> "")
        val result = controller.onPageLoad(pdvOrigin, NormalMode)(request)
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to InvalidDataNINOHelpController page and audit the event when getIdData returns a Left with ConnectorError" in {
      val mockPDVResponseData = PDVResponseData(
        "01234",
        "success",
        Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), None, LocalDate.now())),
        LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      val connectorError = ConnectorError(INTERNAL_SERVER_ERROR, "test")

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseData))
      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Left(connectorError)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          inject.bind[AuditService].toInstance(auditService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(ivOrigin, NormalMode).url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

        verify(auditService, times(1)).audit(any())(any())
      }

    }

    "must redirect to ValidDataNINOMatchedNINOHelpController page when idPostCode equals pdvData.getPostCode" in {
      val mockPDVResponseData = PDVResponseData(
        "01234",
        "success",
        Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), Some("AA1 1AA"), LocalDate.now())),
        LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      val fakeIndividualDetailsWithMatchingPostcode = fakeIndividualDetails.copy(
        accountStatusType = Some(AccountStatusType.FullLive),
        crnIndicator = CrnIndicator.False,
        addressList = AddressList(Some(List(fakeAddress.copy(addressPostcode = Some(AddressPostcode("AA1 1AA"))))))
      )

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseData))
      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithMatchingPostcode)))

      val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      val controller = app.injector.instanceOf[CheckDetailsController]

      running(app) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          .withSession("sessionId" -> "", "credentialId" -> "")

        val resp = controller.onPageLoad(pdvOrigin, NormalMode)(request)
        status(resp) mustEqual SEE_OTHER
        redirectLocation(resp).value mustEqual routes.ValidDataNINOHelpController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to InvalidDataNINOHelpController page when idPostCode does not equals pdvData.getPostCode" in {
      val mockPDVResponseData = PDVResponseData(
        "01234",
        "success",
        Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), Some("AA1 1AA"), LocalDate.now())),
        LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      val fakeIndividualDetailsWithMatchingPostcode = fakeIndividualDetails.copy(
        accountStatusType = Some(AccountStatusType.FullLive),
        crnIndicator = CrnIndicator.False,
        addressList = AddressList(Some(List(fakeAddress.copy(addressPostcode = Some(AddressPostcode("AA1 2AA"))))))
      )

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseData))
      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithMatchingPostcode)))

      val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      val controller = app.injector.instanceOf[CheckDetailsController]

      running(app) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(ivOrigin, NormalMode).url)
          .withSession("sessionId" -> "", "credentialId" -> "")

        val resp = controller.onPageLoad(ivOrigin, NormalMode)(request)
        status(resp) mustEqual SEE_OTHER
        redirectLocation(resp).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
      }
    }

  }
}