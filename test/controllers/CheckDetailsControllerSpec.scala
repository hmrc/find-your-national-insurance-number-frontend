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
import models.errors.ConnectorError
import models.individualdetails._
import models.pdv.{PDVRequest, PDVResponseData}
import models.{AddressLine, CorrelationId, IndividualDetailsResponseEnvelope, NormalMode, individualdetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PersonalDetailsValidationService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpException
import util.AnyValueTypeMatcher.anyValueType
import viewmodels.govuk.SummaryListFluency

import java.time.{LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

// TODO assert audit events get called in specific cases?

class CheckDetailsControllerSpec extends SpecBase with SummaryListFluency {

  implicit val correlationId: models.CorrelationId = models.CorrelationId(UUID.randomUUID())
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val pdvOrigin: Option[String] = Some("PDV")
  val ivOrigin: Option[String] = Some("IV")

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

  val mockPDVResponseData: PDVResponseData = PDVResponseData(
    "01234",
    "success",
    Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), Some("AA1 1AA"), LocalDate.now())),
    LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
  )
  val mockPDVResponseDataFailure: PDVResponseData = mockPDVResponseData.copy(validationStatus = "failure")

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockIndividualDetailsConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
  val controller: CheckDetailsController = application.injector.instanceOf[CheckDetailsController]


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector, mockIndividualDetailsConnector, mockPersonalDetailsValidationService)
  }

  "CheckDetailsController" - {

    "must redirect with http status SEE_OTHER to InvalidDataNINOHelpController when invalid origin" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(Some("foo"), NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
      }
    }

    "pdvData and idData are invalid" - {

      "must redirect with http status SEE_OTHER to InvalidDataNINOHelpController when pdvData status is failure" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataFailure))

        running(application) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
        }
      }

      "must redirect with http status SEE_OTHER to InvalidDataNINOHelpController when pdvData throws http exception" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.failed(new HttpException("something went wrong", INTERNAL_SERVER_ERROR)))

        running(application) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
        }
      }

      "must redirect with http status SEE_OTHER to InvalidDataNINOHelpController when pdvData is valid and idData returns a connection error" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseData))

        when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
          .thenReturn(IndividualDetailsResponseEnvelope(Left(ConnectorError(INTERNAL_SERVER_ERROR, "error"))))

        running(application) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
        }
      }

    }

    "pdvData and idData are valid" - {

      "api1694Check is true" - {

        "pdvData has a postcode" - {

          "must redirect with status SEE_OTHER to ValidDataNINOHelpController when the NPS postcode matches the pdvData postcode" in {
            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
                inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector)
              )
              .build()

            when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
              .thenReturn(Future.successful(mockPDVResponseData))

            when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
              .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

            running(application) {
              val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.ValidDataNINOHelpController.onPageLoad(NormalMode).url
            }
          }

          "must redirect with status SEE_OTHER to InvalidDataNINOHelpController when when the NPS postcode does not match the pdvData postcode" in {
            val mockPDVResponseDataWithValues = mockPDVResponseData.copy(personalDetails =
              Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), Some("AA2 2AA"), LocalDate.now())))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
                inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector)
              )
              .build()

            when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
              .thenReturn(Future.successful(mockPDVResponseDataWithValues))

            when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
              .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

            running(application) {
              val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
            }
          }

        }

        "pvData does not have a postcode" - {

          "must redirect with status SEE_OTHER to ValidDataNINOMatchedNINOHelpController when when pdvData does not have a postcode" in {
            val mockPDVResponseDataWithValues = mockPDVResponseData.copy(personalDetails =
              Some(models.pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), None, LocalDate.now())))

            val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
              .overrides(
                inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
                inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector)
              )
              .build()

            when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
              .thenReturn(Future.successful(mockPDVResponseDataWithValues))

            when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
              .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

            running(application) {
              val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(NormalMode).url
            }
          }
        }
      }

      "api1694Check is false" - {

        "must redirect with status SEE_OTHER to InvalidDataNINOHelpController when AccountStatusType is not FullLive" in {
          val fakeIndividualDetailsWithConditionsNotMet = fakeIndividualDetails.copy(
            accountStatusType = Some(AccountStatusType.Redundant)
          )

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
              inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector)
            )
            .build()

          when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
            .thenReturn(Future.successful(mockPDVResponseData))

          when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
            .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithConditionsNotMet)))

          running(application) {
            val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
          }
        }

        "must redirect with status SEE_OTHER to InvalidDataNINOHelpController when CRN indicator is true" in {
          val fakeIndividualDetailsWithConditionsNotMet = fakeIndividualDetails.copy(
            crnIndicator = CrnIndicator.True
          )

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
              inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector)
            )
            .build()

          when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
            .thenReturn(Future.successful(mockPDVResponseData))

          when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
            .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithConditionsNotMet)))

          running(application) {
            val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
          }
        }

        "must redirect with status SEE_OTHER to InvalidDataNINOHelpController when ResidentialAddressStatus is Dlo" in {
          val fakeIndividualDetailsWithConditionsNotMet = fakeIndividualDetails.copy(
            addressList = AddressList(Some(List(fakeAddress.copy(addressStatus = Some(AddressStatus.Dlo)))))
          )

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
              inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector)
            )
            .build()

          when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
            .thenReturn(Future.successful(mockPDVResponseData))

          when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
            .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithConditionsNotMet)))

          running(application) {
            val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
          }
        }

        "must redirect with status SEE_OTHER to InvalidDataNINOHelpController when ResidentialAddressStatus is Nfa" in {
          val fakeIndividualDetailsWithConditionsNotMet = fakeIndividualDetails.copy(
            addressList = AddressList(Some(List(fakeAddress.copy(addressStatus = Some(AddressStatus.Nfa)))))
          )

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
              inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector)
            )
            .build()

          when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
            .thenReturn(Future.successful(mockPDVResponseData))

          when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
            .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetailsWithConditionsNotMet)))

          running(application) {
            val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
          }
        }
      }
    }

    "must redirect with http status SEE_OTHER to InvalidDataNINOHelpController when the try future fails" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector)
        )
        .build()

      val pdvRequest = PDVRequest("credentialId", "sessionId")

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(pdvRequest)(hc))
        .thenReturn(Future.successful(mockPDVResponseData))

      when(mockIndividualDetailsConnector.getIndividualDetails(any(), anyValueType[ResolveMerge])(any(), any(), anyValueType[CorrelationId]))
        .thenThrow(new InternalError("Something went wrong"))

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
      }
    }
  }
}