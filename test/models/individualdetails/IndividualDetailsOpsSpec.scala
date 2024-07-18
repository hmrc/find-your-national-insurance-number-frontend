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

package models.individualdetails

import models.AddressLine
import models.individualdetails.AddressType.ResidentialAddress
import models.individualdetails.IndividualDetails.IndividualDetailsOps
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.OptionValues

import java.time.LocalDate

class IndividualDetailsOpsSpec extends AnyFlatSpec with Matchers with OptionValues {

  val individualDetails = IndividualDetails(
    "AB123456",
    Some(NinoSuffix("A")),
    Some(AccountStatusType.FullLive),
    Some(LocalDate.of(2000, 1, 1)),
    LocalDate.of(1980, 1, 1),
    Some(DateOfBirthStatus.Verified),
    None,
    None,
    Some(LocalDate.of(2000, 1, 1)),
    CrnIndicator.False,
    NameList(Some(List(Name(
      NameSequenceNumber(1),
      NameType.RealName,
      Some(TitleType.Mr),
      None,
      NameStartDate(LocalDate.of(2000, 1, 1)),
      None,
      None,
      None,
      FirstForename("John"),
      None,
      Surname("Doe")
    )))),
    AddressList(Some(List(Address(
      AddressSequenceNumber(1),
      Some(AddressSource.Customer),
      CountryCode(826),
      ResidentialAddress,
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
      Some(AddressPostcode("AB12CD"))
    ))))
  )

  "IndividualDetailsOps" should "get residential address" in {
    val ops = new IndividualDetailsOps(individualDetails)
    ops.getAddressTypeResidential shouldEqual individualDetails.addressList.getAddress.headOption
  }

  it should "get postcode" in {
    val ops = new IndividualDetailsOps(individualDetails)
    ops.getPostCode shouldEqual "AB12CD"
  }

  it should "get NINO without suffix" in {
    val ops = new IndividualDetailsOps(individualDetails)
    ops.getNinoWithoutSuffix shouldEqual "AB123456"
  }

  it should "get first forename" in {
    val ops = new IndividualDetailsOps(individualDetails)
    ops.getFirstForename shouldEqual "John"
  }

  it should "get last name" in {
    val ops = new IndividualDetailsOps(individualDetails)
    ops.getLastName shouldEqual "Doe"
  }
}