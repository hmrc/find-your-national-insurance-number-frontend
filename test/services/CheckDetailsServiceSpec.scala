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

package services

import config.FrontendAppConfig
import connectors.IndividualDetailsConnector
import models.errors.ConnectorError
import models.{AddressLine, CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope, individualdetails}
import models.individualdetails._
import models.pdv.{PDVRequest, PDVResponseData}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.mock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.mockito.MockitoSugar
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import util.AnyValueTypeMatcher.anyValueType

import java.time.{LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import scala.concurrent.Future

class CheckDetailsServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  import CheckDetailsServiceSpec._
  override def beforeEach(): Unit = {
    reset(personalDetailsValidationService, mockFrontendAppConfig, individualDetailsConnector)
  }

  "CheckDetailsService" must {

    "CheckDetailsService.checkConditions" must {

      "return true and empty string when all conditions are met" in {
        val fakeIndividualDetailsWithConditionsMet = fakeIndividualDetails.copy(
          accountStatusType = Some(AccountStatusType.FullLive),
          crnIndicator = CrnIndicator.False,
          addressList = AddressList(Some(List(
            fakeAddress.copy(addressStatus = Some(AddressStatus.NotDlo)))
          ))
        )

        val result = npsFMNService.checkConditions(fakeIndividualDetailsWithConditionsMet)
        result mustBe (true, "")
      }

      "return false and reason when accountStatusType is not FullLive" in {
        val fakeIndividualDetailsWithFewConditionsNotMet = fakeIndividualDetails.copy(
          accountStatusType = Some(AccountStatusType.FullCancelled),
          crnIndicator = CrnIndicator.False,
          addressList = AddressList(Some(List(
            fakeAddress.copy(
              addressType = AddressType.ResidentialAddress,
              addressStatus = Some(AddressStatus.NotDlo)
            ))
          ))
        )

        val result = npsFMNService.checkConditions(fakeIndividualDetailsWithFewConditionsNotMet)
        result mustBe (false, "AccountStatusType is not FullLive;")
      }

      "return false and reason when crnIndicator is True" in {
        val fakeIndividualDetailsWithFewConditionsNotMet = fakeIndividualDetails.copy(
          accountStatusType = Some(AccountStatusType.FullLive),
          crnIndicator = CrnIndicator.True,
          addressList = AddressList(Some(List(
            fakeAddress.copy(
              addressType = AddressType.ResidentialAddress,
              addressStatus = Some(AddressStatus.NotDlo)
            ))
          ))
        )

        val result = npsFMNService.checkConditions(fakeIndividualDetailsWithFewConditionsNotMet)
        result mustBe (false, "CRN;")
      }

      "return false and reason when addressStatus is Dlo" in {
        val fakeIndividualDetailsWithFewConditionsNotMet = fakeIndividualDetails.copy(
          accountStatusType = Some(AccountStatusType.FullLive),
          crnIndicator = CrnIndicator.False,
          addressList = AddressList(Some(List(
            fakeAddress.copy(
              addressType = AddressType.ResidentialAddress,
              addressStatus = Some(AddressStatus.Dlo)
            ))
          ))
        )
        val result = npsFMNService.checkConditions(fakeIndividualDetailsWithFewConditionsNotMet)
        result mustBe (false, "ResidentialAddressStatus is Dlo or Nfa;")
      }

    }

    "CheckDetailsService.getNPSPostCode" must {

        "return the post code from the residential address" in {
          val result = npsFMNService.getNPSPostCode(fakeIndividualDetails)
          result mustBe "AA1 1AA"
        }

    }

    "CheckDetailsService.getIdData" must {
      "return IndividualDetails when IndividualDetailsConnector returns a successful response" in {
        val mockPDVResponseData = PDVResponseData(
          "1234567890",
          "success",
          Some(models.pdv.PersonalDetails("Abc", "Pqr", Nino("AA123456D"), None, LocalDate.now())),
          LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
        )

        when(individualDetailsConnector.getIndividualDetails(
          IndividualDetailsNino(any[String]), anyValueType[ResolveMerge]
        )(any(), any(), anyValueType[CorrelationId]))
          .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

        val result = npsFMNService.getIdData(mockPDVResponseData)(hc)

        result.map {
          case Right(individualDetails) => individualDetails mustBe fakeIndividualDetails
          case _ => fail("Expected a Right with IndividualDetails, but got a Left")
        }
      }

      "return IndividualDetailsError when IndividualDetailsConnector returns an error" in {
        val mockPDVResponseData = PDVResponseData(
          "1234567890",
          "success",
          Some(models.pdv.PersonalDetails("Abc", "Pqr", Nino("AA123456D"), None, LocalDate.now())),
          LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
        )

        when(individualDetailsConnector.getIndividualDetails(IndividualDetailsNino(any[String]), anyValueType[ResolveMerge])
        (any(), any(), anyValueType[CorrelationId]))
          .thenReturn(IndividualDetailsResponseEnvelope(Left(ConnectorError(INTERNAL_SERVER_ERROR, "error"))))

        val result = npsFMNService.getIdData(mockPDVResponseData)

        result.map {
          case Left(individualDetailsError) => individualDetailsError mustBe ConnectorError(INTERNAL_SERVER_ERROR, "error")
          case _ => fail("Expected a Left with IndividualDetailsError, but got a Right")
        }
      }
    }

    "CheckDetailsService.getPDVData" must {

      "return PDVResponseData when personalDetailsValidationService returns a successful response" in {
        val mockPDVRequest = PDVRequest("1234567890", "1234567890")

        val pdvResData = PDVResponseData(
          "1234567890",
          "success",
          Some(models.pdv.PersonalDetails("Abc", "Pqr", Nino("AA123456D"), None, LocalDate.now())),
          LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
        )
        when(personalDetailsValidationService.createPDVDataFromPDVMatch(mockPDVRequest)(hc))
          .thenReturn(Future.successful(pdvResData))

        val result = npsFMNService.getPDVData(mockPDVRequest)

        result.map { pdvResponseData =>
          pdvResponseData mustBe pdvResData
        }
      }

    }

  }

}

object CheckDetailsServiceSpec {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier         = HeaderCarrier()
  private val mockFrontendAppConfig = mock[FrontendAppConfig]
  private val personalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
  private val individualDetailsConnector: IndividualDetailsConnector = mock[IndividualDetailsConnector]
  private val npsFMNService = new CheckDetailsServiceImpl(personalDetailsValidationService,
    individualDetailsConnector)(global, mockFrontendAppConfig)

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

}