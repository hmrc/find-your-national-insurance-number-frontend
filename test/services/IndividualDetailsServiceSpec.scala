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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._

import scala.concurrent.Future
import models.individualdetails.{AccountStatusType, Address, AddressList, AddressPostcode, AddressSequenceNumber, AddressSource, AddressStatus, AddressType, CountryCode, CrnIndicator, DateOfBirthStatus, DateOfDeathStatus, DeliveryInfo, FirstForename, Honours, IndividualDetails, IndividualDetailsData, IndividualDetailsDataCache, Name, NameEndDate, NameList, NameSequenceNumber, NameStartDate, NameType, NinoSuffix, OtherTitle, PafReference, RequestedName, SecondForename, Surname, TitleType, VpaMail}
import models.AddressLine
import org.mockito.ArgumentMatchers.any
import org.mongodb.scala.MongoException
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import repositories.EncryptedIndividualDetailsRepository
import services.IndividualDetailsServiceImpl

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class IndividualDetailsServiceSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  val fakeName = Name(
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

  val fakeAddress = Address(
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

  val fakeIndividualDetails = IndividualDetails(
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

  "IndividualDetailsService" should "create individual details data" in {
    val mockRepo = mock[EncryptedIndividualDetailsRepository]
    val service = new IndividualDetailsServiceImpl(mockRepo)

    val individualDetails = fakeIndividualDetails
    val sessionId = "12345"

    when(mockRepo.insertOrReplaceIndividualDetailsData(any())(any()))
      .thenReturn(Future.successful("Success"))

    val result = service.createIndividualDetailsData(sessionId, individualDetails)

    result.futureValue shouldBe "Success"
  }

  it should "get individual details data" in {
    val mockRepo = mock[EncryptedIndividualDetailsRepository]
    val service = new IndividualDetailsServiceImpl(mockRepo)

    val nino = "AB123456C"
    val individualDetailsDataCache = IndividualDetailsDataCache("12345", Some(IndividualDetailsData("John", "Doe", "1980-01-01", "AB12CD", "AB123456C")))

    when(mockRepo.findIndividualDetailsDataByNino(any())(any()))
      .thenReturn(Future.successful(Some(individualDetailsDataCache)))

    val result = service.getIndividualDetailsData(nino)

    result.futureValue shouldBe Some(individualDetailsDataCache)
  }

  "getIndividualDetailsData" should "return data when the repository returns some data" in {
    val mockRepo = mock[EncryptedIndividualDetailsRepository]
    val service = new IndividualDetailsServiceImpl(mockRepo)
    val nino = "AB123456C"
    val individualDetailsDataCache = Some(IndividualDetailsDataCache("12345", None))

    when(mockRepo.findIndividualDetailsDataByNino(nino)).thenReturn(Future.successful(individualDetailsDataCache))

    val result = service.getIndividualDetailsData(nino)

    result.futureValue shouldBe individualDetailsDataCache
  }

  it should "return None when the repository throws a MongoException" in {
    val mockRepo = mock[EncryptedIndividualDetailsRepository]
    val service = new IndividualDetailsServiceImpl(mockRepo)
    val nino = "AB123456C"

    when(mockRepo.findIndividualDetailsDataByNino(nino)).thenReturn(Future.failed(new MongoException("Mongo exception")))

    val result = service.getIndividualDetailsData(nino)

    result.futureValue shouldBe None
  }

}