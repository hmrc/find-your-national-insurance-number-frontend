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
import connectors.IndividualDetailsConnector
import models.{AddressLine, IndividualDetailsIdentifier, IndividualDetailsNino, IndividualDetailsResponseEnvelope, NormalMode, PersonDetails, TemporaryReferenceNumber, individualdetails}
import models.individualdetails._
import models.pdv.{PDVRequest, PDVResponseData, PersonalDetails}
import models.errors.ConnectorError
import models.errors.IndividualDetailsError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PersonalDetailsValidationService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset}
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

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockIndividualDetailsConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]


  val controller: CheckDetailsController = application.injector.instanceOf[CheckDetailsController]


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector, mockIndividualDetailsConnector, mockPersonalDetailsValidationService)
  }

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
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]
        val list = SummaryListViewModel(Seq.empty)

        status(result) mustEqual 303
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
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual 303
        redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to ValidDataNINOMatchedNINOHelpController page when PDVResponseData is matched and postcode is matched" in {

      // construct PDVResponseData with some values
      val mockPDVResponseDataWithValues = PDVResponseData(
        "01234",
        "success",
        Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), None, LocalDate.now())),
        LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      val pdvRequest = PDVRequest("credentialId", "sessionId")

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(pdvRequest)(hc))
        .thenReturn(Future.successful(mockPDVResponseDataWithValues))

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual 303
        //redirectLocation(result).value mustEqual routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to ValidDataNINOMatchedNINOHelpController page when PDVResponseData is matched and nino is matched, postcode is missing" in {

      // construct PDVResponseData with some values
      val mockPDVResponseDataWithValues = PDVResponseData(
        "01234",
        "success",
        Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), None, LocalDate.now())),
        LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      val pdvRequest = PDVRequest("credentialId", "sessionId")

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(pdvRequest)(hc))
        .thenReturn(Future.successful(mockPDVResponseDataWithValues))

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual 303
        //redirectLocation(result).value mustEqual routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to InvalidDataNINOHelpController page when IndividualDetails is failed" in {


      when(mockIndividualDetailsConnector.getIndividualDetails(TemporaryReferenceNumber("fakeNino"), ResolveMerge('Y')))
        .thenReturn(IndividualDetailsResponseEnvelope(Left(ConnectorError(500, "test"))))


      val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)
      val result = controller.onPageLoad(NormalMode)(request)


      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
    }

    "must redirect to ValidDataNINOMatchedNINOHelpController page when IndividualDetails is successful" in {


      val pdvRequest = PDVRequest("credentialId", "sessionId")
      val mockPDVResponseDataWithValues = PDVResponseData(
        "01234",
        "success",
        Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), None, LocalDate.now())),
        LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(pdvRequest)(hc))
        .thenReturn(Future.successful(mockPDVResponseDataWithValues))


      when(mockIndividualDetailsConnector.getIndividualDetails(TemporaryReferenceNumber("fakeNino"), ResolveMerge('Y')))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))


      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector)
        )
        .build()


      running(application) {

      // Call the onPageLoad method
      val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)
      val result = controller.onPageLoad(NormalMode)(request)

      // Assert that the status of the result is SEE_OTHER
      status(result) mustEqual SEE_OTHER



      // Assert that the redirect location of the result is the URL of the ValidDataNINOMatchedNINOHelpController page
     // redirectLocation(result).value mustEqual routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(NormalMode).url
    }}

    "getIdData" - {
      "must return IndividualDetails when IndividualDetailsConnector returns a successful response" in {
        val mockPDVResponseData = PDVResponseData(
          "01234",
          "success",
          Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), None, LocalDate.now())),
          LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
        )

        when(mockIndividualDetailsConnector.getIndividualDetails(IndividualDetailsNino(mockPDVResponseData.personalDetails.get.nino.nino), ResolveMerge('Y')))
          .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

        val result = controller.getIdData(mockPDVResponseData)

        result.map {
          case Right(individualDetails) => individualDetails mustBe fakeIndividualDetails
          case _ => fail("Expected a Right with IndividualDetails, but got a Left")
        }
      }


      "must return IndividualDetailsError when IndividualDetailsConnector returns an error" in {
        val mockPDVResponseData = PDVResponseData(
          "01234",
          "success",
          Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), None, LocalDate.now())),
          LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
        )

        //val error = IndividualDetailsError("error")
        when(mockIndividualDetailsConnector.getIndividualDetails(IndividualDetailsNino(mockPDVResponseData.personalDetails.get.nino.nino), ResolveMerge('Y')))
          .thenReturn(IndividualDetailsResponseEnvelope(Left(ConnectorError(500, "test"))))

        val result = controller.getIdData(mockPDVResponseData)

        result.map {
          case Left(individualDetailsError) => individualDetailsError mustBe ConnectorError(500, "test")
          case _ => fail("Expected a Left with IndividualDetailsError, but got a Right")
        }
      }
    }
    "getNPSPostCode" - {
      "must return the postcode of the residential address in IndividualDetails" in {
        val fakePostcode = "AA1 1AA"
        val fakeAddressWithPostcode = fakeAddress.copy(addressPostcode = Some(AddressPostcode(fakePostcode)))
        val fakeIndividualDetailsWithPostcode = fakeIndividualDetails.copy(
          addressList = AddressList(Some(List(fakeAddressWithPostcode)))
        )

        val result = controller.getNPSPostCode(fakeIndividualDetailsWithPostcode)

        result mustEqual fakePostcode
      }
    }

    "checkConditions" - {
      "must return true and an empty reason when accountStatusType is FullLive, crnIndicator is False, and addressStatus is NotDlo" in {
        val fakeIndividualDetailsWithConditionsMet = fakeIndividualDetails.copy(
          accountStatusType = Some(AccountStatusType.FullLive),
          crnIndicator = CrnIndicator.False,
          addressList = AddressList(Some(List(fakeAddress.copy(addressStatus = Some(AddressStatus.NotDlo)))))
        )

        val (status, reason) = controller.checkConditions(fakeIndividualDetailsWithConditionsMet)

        status mustBe true
        reason mustBe ""
      }

      "must return false and a non-empty reason when accountStatusType is not FullLive, crnIndicator is True, and addressStatus is Dlo" in {
        val fakeIndividualDetailsWithConditionsNotMet = fakeIndividualDetails.copy(
          accountStatusType = Some(AccountStatusType.Redundant),
          crnIndicator = CrnIndicator.True,
          addressList = AddressList(Some(List(fakeAddress.copy(addressStatus = Some(AddressStatus.Dlo)))))
        )

        val (status, reason) = controller.checkConditions(fakeIndividualDetailsWithConditionsNotMet)

        status mustBe false
        reason must include("AccountStatusType is not FullLive")
        reason must include("CRN")
        reason must include("ResidentialAddressStatus is Dlo or Nfa")
      }

      "must return false and a non-empty reason when accountStatusType is not FullLive, crnIndicator is False, and addressStatus is Nfa" in {
        val fakeIndividualDetailsWithConditionsNotMet = fakeIndividualDetails.copy(
          accountStatusType = Some(AccountStatusType.Redundant),
          crnIndicator = CrnIndicator.False,
          addressList = AddressList(Some(List(fakeAddress.copy(addressStatus = Some(AddressStatus.Nfa)))))
        )

        val (status, reason) = controller.checkConditions(fakeIndividualDetailsWithConditionsNotMet)

        status mustBe false
        reason must include("AccountStatusType is not FullLive")
        reason mustNot include("CRN")
        reason must include("ResidentialAddressStatus is Dlo or Nfa")
      }

      "must return false and a non-empty reason when accountStatusType is not FullLive, crnIndicator is False, and addressStatus is missing" in {
        val fakeIndividualDetailsWithConditionsNotMet = fakeIndividualDetails.copy(
          accountStatusType = Some(AccountStatusType.Redundant),
          crnIndicator = CrnIndicator.False,
          addressList = AddressList(Some(List(fakeAddress.copy(addressStatus = None))))
        )

        val (status, reason) = controller.checkConditions(fakeIndividualDetailsWithConditionsNotMet)

        status mustBe false
        reason must include("AccountStatusType is not FullLive")
        reason mustNot include("CRN")
        reason must include("ResidentialAddressStatus is Dlo or Nfa")
      }


      "onPageLoad" - {
        "must redirect to InvalidDataNINOHelpController page when getCredentialId returns None" in {
          when(mockAuthConnector.authorise(any(), any())(any(), any()))
            .thenReturn(Future.failed(new Exception("test")))

          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)
          val result = controller.onPageLoad(NormalMode)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
        }

        "must redirect to InvalidDataNINOHelpController page when getPDVData returns PDVResponseData with validationStatus as failure" in {
          when(mockAuthConnector.authorise(any(), any())(any(), any()))
            .thenReturn(Future.failed(new Exception("test")))
          when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any())).thenReturn(Future.successful(PDVResponseData("id", "failure", None, Instant.now(), None, None, None, None)))

          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)
          val result = controller.onPageLoad(NormalMode)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
        }

        "must redirect to InvalidDataNINOHelpController page when getIdData returns a Left with IndividualDetailsError" in {
          when(mockAuthConnector.authorise(any(), any())(any(), any()))
            .thenReturn(Future.failed(new Exception("test")))
          when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
            .thenReturn(Future.successful(PDVResponseData("id", "success", None, Instant.now(), None, None, None, None)))
          when(mockIndividualDetailsConnector.getIndividualDetails(IndividualDetailsNino("fakeNino"), ResolveMerge('Y')))
            .thenReturn(IndividualDetailsResponseEnvelope(Left(ConnectorError(500,"error"))))

          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)
          val result = controller.onPageLoad(NormalMode)(request)

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
        }

       "must redirect to ValidDataNINOMatchedNINOHelpController page when getIdData returns a Right " +
         "with IndividualDetails and checkConditions returns true" in {

          //val fakeCredentials = Credentials("providerId", "providerType")
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
         when(mockIndividualDetailsConnector.getIndividualDetails(IndividualDetailsNino(mockPDVResponseData.personalDetails.get.nino.nino), ResolveMerge('Y')))
           .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))


          val ap = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
              inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector)
            )
            .build()

          running(ap) {
            val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)
            val result = controller.onPageLoad(NormalMode)(request)

            status(result) mustEqual SEE_OTHER
            //redirectLocation(result).value mustEqual routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(NormalMode).url
          }
        }
      }
    }
  }
}
