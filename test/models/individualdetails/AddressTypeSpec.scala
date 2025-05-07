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

import models.individualdetails.AddressType._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class AddressTypeSpec extends AnyFlatSpec with Matchers {

  "AddressType" should "be read correctly from JSON" in {
    val json = Json.parse("""1""")
    json.as[AddressType] shouldBe ResidentialAddress

    val json1 = Json.parse("""2""")
    json1.as[AddressType] shouldBe CorrespondenceAddress
  }

  for {
    (addressType, json) <- Seq(
                             ResidentialAddress    -> """1""",
                             CorrespondenceAddress -> """2"""
                           )
  }
    it should s"write $addressType to JSON" in {
      Json.toJson(addressType.asInstanceOf[AddressType]) shouldBe Json.parse(json)
    }
}
