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
import models.individualdetails._

import java.time.LocalDate

class AddressSpec extends AnyFlatSpec with Matchers {

    def fakeAddress: Address = {
        Address(
                AddressSequenceNumber(1),
                Some(AddressSource.Customer),
                CountryCode(1),
                AddressType.ResidentialAddress,
                Some(AddressStatus.NotDlo),
                LocalDate.now(),
                Some(LocalDate.now().plusDays(1)),
                Some(LocalDate.now()),
                Some(VpaMail(1)),
                Some(DeliveryInfo("Delivery Info")),
                Some(PafReference("PAF Reference")),
                AddressLine("Fake Line 1"),
                AddressLine("Fake Line 2"),
                Some(AddressLine("Fake Line 3")),
                Some(AddressLine("Fake Line 4")),
                Some(AddressLine("Fake Line 5")),
                Some(AddressPostcode("Fake Postcode"))
        )
    }

  "An Address" should "have an addressSequenceNumber" in {
        fakeAddress.addressSequenceNumber should be (AddressSequenceNumber(1))
    }

    it should "have a countryCode" in {
        fakeAddress.countryCode should be (CountryCode(1))
    }

    it should "have an addressType" in {
        fakeAddress.addressType should be (AddressType.ResidentialAddress)
    }

    it should "have an addressStatus" in {
        fakeAddress.addressStatus should be (Some(AddressStatus.NotDlo))
    }

    it should "have an addressStartDate" in {
        fakeAddress.addressStartDate should be (LocalDate.now())
    }

    it should "have an addressEndDate" in {
        fakeAddress.addressEndDate should be (Some(LocalDate.now().plusDays(1)))
    }

    it should "have an addressLastConfirmedDate" in {
        fakeAddress.addressLastConfirmedDate should be (Some(LocalDate.now()))
    }

    it should "have a vpaMail" in {
        fakeAddress.vpaMail should be (Some(VpaMail(1)))
    }

    it should "have a deliveryInfo" in {
        fakeAddress.deliveryInfo should be (Some(DeliveryInfo("Delivery Info")))
    }

    it should "have a pafReference" in {
        fakeAddress.pafReference should be (Some(PafReference("PAF Reference")))
    }

    it should "have an addressLine1" in {
        fakeAddress.addressLine1 should be (AddressLine("Fake Line 1"))
    }

    it should "have an addressLine2" in {
        fakeAddress.addressLine2 should be (AddressLine("Fake Line 2"))
    }

    it should "have an addressLine3" in {
        fakeAddress.addressLine3 should be (Some(AddressLine("Fake Line 3")))
    }

    it should "have an addressLine4" in {
        fakeAddress.addressLine4 should be (Some(AddressLine("Fake Line 4")))
    }

    it should "have an addressLine5" in {
        fakeAddress.addressLine5 should be (Some(AddressLine("Fake Line 5")))
    }

    it should "have an addressPostcode" in {
        fakeAddress.addressPostcode should be (Some(AddressPostcode("Fake Postcode")))
    }
}