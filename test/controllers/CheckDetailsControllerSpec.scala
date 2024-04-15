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
import models.individualdetails._
import models.pdv.{PDVNotFoundResponse, PDVRequest, PDVResponse, PDVResponseData, PDVSuccessResponse, PersonalDetails}
import models.{AddressLine, NormalMode, individualdetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AuditService, CheckDetailsService, IndividualDetailsService, PersonalDetailsValidationService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpResponse
import viewmodels.govuk.SummaryListFluency

import java.time._
import scala.concurrent.Future

class CheckDetailsControllerSpec extends SpecBase with SummaryListFluency {

  import CheckDetailsControllerSpec._

  val controller: CheckDetailsController = application.injector.instanceOf[CheckDetailsController]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockAuthConnector,
      mockIndividualDetailsService,
      mockPersonalDetailsValidationService,
      auditService,
      mockCheckDetailsService
    )
  }

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

        verify(auditService, times(1)).start()(any())
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

        verify(auditService, times(1)).start()(any())
      }

      "when PDVResponseData validationStatus is failure" in {

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[CheckDetailsService].toInstance(mockCheckDetailsService),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start()(any())
        }
      }

      "when pdvData throws a runtime exception" in {
        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[CheckDetailsService].toInstance(mockCheckDetailsService),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("Something went wrong")))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start()(any())
        }
      }

      "and audit the event when getIdData returns a Left with ConnectorError" in {
        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[CheckDetailsService].toInstance(mockCheckDetailsService),
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(ivOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start()(any())
        }
      }

      "when idPostCode does not equal pdvData.getPostCode" in {

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[CheckDetailsService].toInstance(mockCheckDetailsService),
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

          verify(auditService, times(1)).start()(any())
        }
      }

      "when AccountStatusType is not FullLive" in {

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start()(any())
        }
      }

      "when CRN indicator is true" in {

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[CheckDetailsService].toInstance(mockCheckDetailsService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start()(any())
        }
      }

      "when ResidentialAddressStatus is Dlo" in {

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[CheckDetailsService].toInstance(mockCheckDetailsService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start()(any())
        }
      }

      "when ResidentialAddressStatus is Nfa" in {

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[CheckDetailsService].toInstance(mockCheckDetailsService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)

          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start()(any())
        }
      }

      "when the try future fails" in {
        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[CheckDetailsService].toInstance(mockCheckDetailsService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        val pdvRequest = PDVRequest("credentialId", "sessionId")
        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(pdvRequest)(hc))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start()(any())
        }
      }

      "when PDV data not found" in {
        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[CheckDetailsService].toInstance(mockCheckDetailsService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(
            PDVNotFoundResponse(HttpResponse(404, "No association found"))
          ))

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start()(any())
        }

      }

    }

    "must redirect to ValidDataNINOMatchedNINOHelpController" - {

      "when pdvData does not have a postcode" in {

        import scala.concurrent.ExecutionContext.Implicits.global
        val mockPDVResponseDataSuccessWithoutNino = mockPDVResponseDataSuccess.copy(
          pdvResponseData = mockPDVResponseDataSuccess.pdvResponseData.copy(
            personalDetails = Some(fakePersonDetails.copy(postCode = None))
          )
        )

        when(mockPersonalDetailsValidationService.getPDVData(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccessWithoutNino))

        when(mockIndividualDetailsService.getIdData(any[PDVResponse])(any()))
          .thenReturn(Future(Right(fakeIndividualDetails)))

        when(mockIndividualDetailsService.getNPSPostCode(any()))
          .thenReturn("AA1 1AA")

        when(mockCheckDetailsService.checkConditions(any()))
          .thenReturn((true,""))

        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[CheckDetailsService].toInstance(mockCheckDetailsService),
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value
          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start()(any())
          verify(auditService, times(1)).findYourNinoPDVMatched(any(), any(), any())(any())
        }
      }

      "when idPostCode equals pdvData.getPostCode" in {
        import scala.concurrent.ExecutionContext.Implicits.global
        when(mockPersonalDetailsValidationService.getPDVData(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))

        when(mockIndividualDetailsService.getIdData(any[PDVResponse])(any()))
          .thenReturn(Future(Right(fakeIndividualDetails)))

        when(mockIndividualDetailsService.getNPSPostCode(any()))
          .thenReturn("AA1 1AA")

        when(mockCheckDetailsService.checkConditions(any()))
          .thenReturn((true,""))

        when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
          .thenReturn(Future.successful(mockPDVResponseDataSuccess))


        val app = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[CheckDetailsService].toInstance(mockCheckDetailsService),
            inject.bind[IndividualDetailsService].toInstance(mockIndividualDetailsService),
            inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
            inject.bind[AuditService].toInstance(auditService)
          )
          .build()

        running(app) {
          val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(pdvOrigin, NormalMode).url)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.ValidDataNINOHelpController.onPageLoad(NormalMode).url

          verify(auditService, times(1)).start()(any())
          verify(auditService, times(1)).findYourNinoPDVMatched(any(), any(), any())(any())
        }
      }

    }

  }

}

object CheckDetailsControllerSpec {

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

  val mockPDVResponseDataSuccess: PDVSuccessResponse = PDVSuccessResponse(PDVResponseData(
    "01234",
    "success",
    Some(fakePersonDetails),
    LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
  ))

  val headers: Map[String, Seq[String]] = Map(
    "CorrelationId" -> Seq("1118057e-fbbc-47a8-a8b4-78d9f015c253"),
    "Content-Type" -> Seq("application/json")
  )

  val body: String =
    s"""
       |{
       |  "id": "Foo",
       |  "validationStatus": "failure"
       |}
       |""".stripMargin

  val httpResponse: HttpResponse = HttpResponse(200, body, headers)

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockIndividualDetailsService: IndividualDetailsService = mock[IndividualDetailsService]
  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
  val auditService: AuditService = mock[AuditService]
  val mockCheckDetailsService: CheckDetailsService = mock[CheckDetailsService]

  val pdvOrigin: Option[String] = Some("PDV")
  val ivOrigin: Option[String] = Some("IV")
}