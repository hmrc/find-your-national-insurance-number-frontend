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

import connectors.DefaultIndividualDetailsConnector
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._

import scala.concurrent.Future
import models.individualdetails.{AccountStatusType, Address, AddressList, AddressPostcode, AddressSequenceNumber, AddressSource, AddressStatus, AddressType, CountryCode, CrnIndicator, DateOfBirthStatus, DateOfDeathStatus, DeliveryInfo, FirstForename, Honours, IndividualDetails, IndividualDetailsData, IndividualDetailsDataCache, Name, NameEndDate, NameList, NameSequenceNumber, NameStartDate, NameType, NinoSuffix, OtherTitle, PafReference, RequestedName, ResolveMerge, SecondForename, Surname, TitleType, VpaMail}
import models.{AddressLine, CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope}
import models.errors.ConnectorError
import models.pdv.{PDVResponseData, PDVSuccessResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.mock
import org.mongodb.scala.MongoException
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.wordspec.AsyncWordSpec
import play.api.http.Status.INTERNAL_SERVER_ERROR
import repositories.{EncryptedIndividualDetailsRepository, SessionRepository}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import util.AnyValueTypeMatcher.anyValueType

import java.time.{LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global

class IndividualDetailsServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  import IndividualDetailsServiceSpec._

  "IndividualDetailsService" should {

    "create individual details data" in {
      val individualDetails = fakeIndividualDetails
      val sessionId = "12345"

      when(individualDetailsRepository.insertOrReplaceIndividualDetailsData(any())(any()))
        .thenReturn(Future.successful("Success"))

      val result = service.createIndividualDetailsData(sessionId, individualDetails)

      result.futureValue shouldBe "Success"
    }

    "get individual details data" in {

      val nino = "AB123456C"
      val individualDetailsDataCache = IndividualDetailsDataCache("12345", Some(IndividualDetailsData("John", "Doe", "1980-01-01", "AB12CD", "AB123456C")))

      when(individualDetailsRepository.findIndividualDetailsDataByNino(any())(any()))
        .thenReturn(Future.successful(Some(individualDetailsDataCache)))

      val result = service.getIndividualDetailsData(nino)

      result.futureValue shouldBe Some(individualDetailsDataCache)
    }

    "getIndividualDetailsData should return data when the repository returns some data" in {
      val nino = "AB123456C"
      val individualDetailsDataCache = Some(IndividualDetailsDataCache("12345", None))

      when(individualDetailsRepository.findIndividualDetailsDataByNino(nino)(global)).thenReturn(Future.successful(individualDetailsDataCache))

      val result = service.getIndividualDetailsData(nino)

      result.futureValue shouldBe individualDetailsDataCache
    }

    "return None when the repository throws a MongoException" in {
      val nino = "AB123456C"

      when(individualDetailsRepository.findIndividualDetailsDataByNino(nino)(global)).thenReturn(Future.failed(new MongoException("Mongo exception")))

      val result = service.getIndividualDetailsData(nino)

      result.futureValue shouldBe None
    }

  }

  "IndividualDetailsService.getNPSPostCode" must {

    "return the post code from the residential address" in {
      val result = service.getNPSPostCode(fakeIndividualDetails)
      result shouldBe "FakePostcode"
    }

  }

  "IndividualDetailsService.getIdData" must {
    "return IndividualDetails when IndividualDetailsConnector returns a successful response" in {
      val mockPDVResponseData =
        PDVResponseData(
          "1234567890",
          "success",
          Some(models.pdv.PersonalDetails("Abc", "Pqr", Nino("AA123456D"), None, LocalDate.now())),
          LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      when(individualDetailsConnector.getIndividualDetails(
        IndividualDetailsNino(any[String]), anyValueType[ResolveMerge]
      )(any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

      val result = service.getIdData(mockPDVResponseData)

      result.map {
        case Right(individualDetails) => individualDetails shouldBe fakeIndividualDetails
        case _ => fail("Expected a Right with IndividualDetails, but got a Left")
      }(global)
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

      val result = service.getIdData(mockPDVResponseData)

      result.map {
        case Left(individualDetailsError) => individualDetailsError shouldBe ConnectorError(INTERNAL_SERVER_ERROR, "error")
        case _ => fail("Expected a Left with IndividualDetailsError, but got a Right")
      }(global)
    }
  }

  "IndividualDetailsService.getIndividualDetailsAddress" must {

    "return Address when getIndividualDetails returns a successful response" in {
      when(individualDetailsConnector.getIndividualDetails(IndividualDetailsNino(any[String]), anyValueType[ResolveMerge])
      (any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Right(fakeIndividualDetails)))

      val result = service.getIndividualDetailsAddress(IndividualDetailsNino("AB123456C"))(global, hc)

      result.map {
        case Right(address) => address shouldBe fakeAddress
        case _ => fail("Expected a Right with Address, but got a Left")
      }(global)
    }

    "return IndividualDetailsError when getIndividualDetails returns an error" in {
      when(individualDetailsConnector.getIndividualDetails(IndividualDetailsNino(any[String]), anyValueType[ResolveMerge])
      (any(), any(), anyValueType[CorrelationId]))
        .thenReturn(IndividualDetailsResponseEnvelope(Left(ConnectorError(INTERNAL_SERVER_ERROR, "error"))))

      val result = service.getIndividualDetailsAddress(IndividualDetailsNino("AB123456C"))(global, hc)

      result.map {
        case Left(individualDetailsError) => individualDetailsError shouldBe ConnectorError(INTERNAL_SERVER_ERROR, "error")
        case _ => fail("Expected a Left with IndividualDetailsError, but got a Right")
      }(global)
    }

  }

}

object IndividualDetailsServiceSpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val fakeName: Name = Name(
    NameSequenceNumber(1),
    NameType.RealName,
    Some(TitleType.Mr),
    Some(RequestedName("FakeRequestedName")),
    NameStartDate(LocalDate.of(2000, 1, 1)),
    Some(NameEndDate(LocalDate.of(2020, 1, 1))),
    Some(OtherTitle("FakeOtherTitle")),
    Some(Honours("FakeHonours")),
    FirstForename("FakeFirstName"),
    Some(SecondForename("FakeSecondName")),
    Surname("FakeLastName"))

  val fakeAddress: Address = Address(
    AddressSequenceNumber(1),
    Some(AddressSource.Customer),
    CountryCode(826),
    AddressType.ResidentialAddress,
    Some(AddressStatus.NotDlo),
    LocalDate.of(2000, 1, 1),
    Some(LocalDate.of(2020, 1, 1)),
    Some(LocalDate.of(2000, 1, 1)),
    Some(VpaMail(0)),
    Some(DeliveryInfo("FakeDeliveryInfo")),
    Some(PafReference("FakePafReference")),
    AddressLine("FakeStreet"),
    AddressLine("FakeTown"),
    Some(AddressLine("FakeCity")),
    Some(AddressLine("FakeCounty")),
    Some(AddressLine("FakeCountry")),
    Some(AddressPostcode("FakePostcode"))
  )

  val fakeIndividualDetails: IndividualDetails = IndividualDetails(
    "FakeNinoWithoutSuffix",
    Some(NinoSuffix("A")),
    Some(AccountStatusType.FullLive),
    Some(LocalDate.of(2000, 1, 1)),
    LocalDate.of(2000, 1, 1),
    Some(DateOfBirthStatus.Verified),
    Some(LocalDate.of(2020, 1, 1)),
    Some(DateOfDeathStatus.Verified),
    Some(LocalDate.of(2000, 1, 1)),
    CrnIndicator.False,
    NameList(Some(List(fakeName))),
    AddressList(Some(List(fakeAddress)))
  )

  val individualDetailsRepository: EncryptedIndividualDetailsRepository = mock[EncryptedIndividualDetailsRepository]
  val individualDetailsConnector: DefaultIndividualDetailsConnector = mock[DefaultIndividualDetailsConnector]
  val repository: SessionRepository = mock[SessionRepository]
  val service = new IndividualDetailsServiceImpl(individualDetailsConnector, individualDetailsRepository, repository)
}