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
import connectors.{IndividualDetailsConnector, PersonalDetailsValidationConnector}
import models.errors.ConnectorError
import models.individualdetails._
import models.pdv.{PDVRequest, PDVResponse, PDVResponseData, PDVSuccessResponse, PersonalDetails}
import models.{AddressLine, CorrelationId, IndividualDetailsResponseEnvelope, NormalMode, individualdetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AuditService, PersonalDetailsValidationService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpException
import util.AnyValueTypeMatcher.anyValueType
import viewmodels.govuk.SummaryListFluency

import java.time._
import scala.concurrent.Future

class CheckDetailsControllerSpec extends SpecBase with SummaryListFluency {

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

  val mockPDVResponseDataSuccess: PDVResponseData = PDVResponseData(
    "01234",
    "success",
    Some(fakePersonDetails),
    LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
  )
  val mockPDVResponseDataFailure: PDVResponseData = PDVResponseData(
    "01234",
    "failure",
    None,
    LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
  )

  val pvdResponse: PDVResponse = PDVSuccessResponse(mockPDVResponseDataFailure)

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockIndividualDetailsConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
  val controller: CheckDetailsController = application.injector.instanceOf[CheckDetailsController]
  val auditService: AuditService = mock[AuditService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockAuthConnector,
      mockIndividualDetailsConnector,
      mockPersonalDetailsValidationService,
      auditService
    )
  }

  val pdvOrigin: Option[String] = Some("PDV")
  val ivOrigin: Option[String] = Some("IV")

  "CheckDetailsController" - {
   
    "must redirect to InvalidDataNINOHelpController" - {
      
      "when invalid origin" in {

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        val invalidOrigin: Option[String] = Some("test")

        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(invalidOrigin, NormalMode).url)
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

        verify(auditService, times(1)).start(any())(any())
      }

      "when credentialId is empty" in {

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          .withSession("sessionId" -> "", "credentialId" -> "")
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

        verify(auditService, times(1)).start(any())(any())
      }

      "when PDVResponseData validationStatus is failure" in {

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.getPDVMatchResult(any())(any()))
          .thenReturn(Future.successful(pvdResponse))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start(any())(any())
          verify(auditService, times(1)).findYourNinoPDVMatchFailed(any(), any())(any())
        }
      }

      "when pdvData throws http exception" in {
        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.failed(new HttpException("something went wrong", INTERNAL_SERVER_ERROR)))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start(any())(any())
          verify(auditService, times(1)).findYourNinoGetPdvDataHttpError(any())(any())
        }
      }

      "and audit the event when getIdData returns a Left with ConnectorError" in {
        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))
        when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
          .thenReturn(IndividualDetailsResponseEnvelope(Left(ConnectorError(INTERNAL_SERVER_ERROR, "test"))))

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(ivOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start(any())(any())
          verify(auditService, times(1)).findYourNinoIdDataError(any(), any(), any(), any())(any())
        }
      }

      "when idPostCode does not equal pdvData.getPostCode" in {
        val fakeIndividualDetailsWithMatchingPostcode = fakeIndividualDetails.copy(
          addressList = AddressList(Some(List(fakeAddress.copy(addressPostcode = Some(AddressPostcode("  A A1 2AA   "))))))
        )

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))
        when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
          .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithMatchingPostcode)))

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(ivOrigin, NormalMode).url)
            .withSession("sessionId" -> "", "credentialId" -> "")
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start(any())(any())
          verify(auditService, times(1)).findYourNinoPDVMatched(any(),any(),any())(any())
        }
      }

      "when AccountStatusType is not FullLive" in {
        val fakeIndividualDetailsWithConditionsNotMet = fakeIndividualDetails.copy(
          accountStatusType = Some(AccountStatusType.Redundant)
        )

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))
        when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
          .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithConditionsNotMet)))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start(any())(any())
          verify(auditService, times(1)).findYourNinoPDVMatched(any(), any(), any())(any())
        }
      }

      "when CRN indicator is true" in {
        val fakeIndividualDetailsWithConditionsNotMet = fakeIndividualDetails.copy(
          crnIndicator = CrnIndicator.True
        )

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))
        when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
          .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithConditionsNotMet)))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start(any())(any())
          verify(auditService, times(1)).findYourNinoPDVMatched(any(), any(), any())(any())
        }
      }

      "when ResidentialAddressStatus is Dlo" in {
        val fakeIndividualDetailsWithConditionsNotMet = fakeIndividualDetails.copy(
          addressList = AddressList(Some(List(fakeAddress.copy(addressStatus = Some(AddressStatus.Dlo)))))
        )

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))
        when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
          .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithConditionsNotMet)))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start(any())(any())
          verify(auditService, times(1)).findYourNinoPDVMatched(any(), any(), any())(any())
        }
      }

      "when ResidentialAddressStatus is Nfa" in {
        val fakeIndividualDetailsWithConditionsNotMet = fakeIndividualDetails.copy(
          addressList = AddressList(Some(List(fakeAddress.copy(addressStatus = Some(AddressStatus.Nfa)))))
        )

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))
        when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
          .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithConditionsNotMet)))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start(any())(any())
          verify(auditService, times(1)).findYourNinoPDVMatched(any(), any(), any())(any())
        }
      }

      "when the try future fails" in {
        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        val pdvRequest = PDVRequest("credentialId", "sessionId")
        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(pdvRequest)(hc))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))
        when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
          .thenThrow(new InternalError("Something went wrong"))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start(any())(any())
        }
      }
    }



    "must redirect to ValidDataNINOMatchedNINOHelpController when pdvData does not have a postcode" in {
      val mockPDVResponseDataSuccessWithoutNino = mockPDVResponseDataSuccess.copy(personalDetails = Some(fakePersonDetails.copy(postCode = None)))

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseDataSuccessWithoutNino))
      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

      val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          inject.bind[AuditService].toInstance(auditService)
        )
        .build()

      running(app) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
        val result = route(app, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(NormalMode).url

        verify(auditService, times(1)).start(any())(any())
        verify(auditService, times(1)).findYourNinoPDVMatched(any(), any(), any())(any())
      }
    }

    "must redirect to ValidDataNINOHelpController when idPostCode equals pdvData.getPostCode" in {
      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseDataSuccess))
      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

      val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          inject.bind[AuditService].toInstance(auditService)
        )
        .build()

      running(app) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ValidDataNINOHelpController.onPageLoad(NormalMode).url

        verify(auditService, times(1)).start(any())(any())
        verify(auditService, times(1)).findYourNinoPDVMatched(any(), any(), any())(any())
      }
    }
  }
}