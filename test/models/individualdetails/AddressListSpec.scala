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
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDate

class AddressListOpsSpec extends AnyFlatSpec with Matchers {
  val fakeAddress = Address(
    addressSequenceNumber = AddressSequenceNumber(1),
    addressSource = Some(AddressSource.Customer),
    countryCode = CountryCode(1),
    addressType = AddressType.ResidentialAddress,
    addressStatus = Some(AddressStatus.NotDlo),
    addressStartDate = LocalDate.now(),
    addressEndDate = Some(LocalDate.now().plusDays(10)),
    addressLastConfirmedDate = Some(LocalDate.now().plusDays(5)),
    vpaMail = Some(VpaMail(1)),
    deliveryInfo = Some(DeliveryInfo("Delivery Info")),
    pafReference = Some(PafReference("PAF Reference")),
    addressLine1 = AddressLine("Fake Line 1"),
    addressLine2 = AddressLine("Fake Line 2"),
    addressLine3 = Some(AddressLine("Fake Line 3")),
    addressLine4 = Some(AddressLine("Fake Line 4")),
    addressLine5 = Some(AddressLine("Fake Line 5")),
    addressPostcode = Some(AddressPostcode("Fake Postcode"))
  )

  "getAddress" should "return the list of addresses when the AddressList object has some addresses" in {
    val addresses = List(fakeAddress, fakeAddress)
    val addressList = AddressList(Some(addresses))
    addressList.getAddress shouldEqual addresses
  }

  it should "return an empty list when the AddressList object doesn't have any addresses" in {
    val addressList = AddressList(None)
    addressList.getAddress shouldEqual List.empty[Address]
  }
}