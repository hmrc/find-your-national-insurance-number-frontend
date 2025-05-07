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

import models.individualdetails.AddressSource._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class AddressSourceSpec extends AnyFlatSpec with Matchers {

  "AddressSource" should "be read correctly from JSON" in {
    val json = Json.parse("""0""")
    json.as[AddressSource] shouldBe NotKnown

    val json1 = Json.parse("""1""")
    json1.as[AddressSource] shouldBe Customer

    val json2 = Json.parse("""2""")
    json2.as[AddressSource] shouldBe Relative

    val json3 = Json.parse("""3""")
    json3.as[AddressSource] shouldBe Employer

    val json4 = Json.parse("""4""")
    json4.as[AddressSource] shouldBe InlandRevenue

    val json5 = Json.parse("""5""")
    json5.as[AddressSource] shouldBe OtherGovernmentDepartment

    val json6 = Json.parse("""6""")
    json6.as[AddressSource] shouldBe OtherThirdParty

    val json7 = Json.parse("""7""")
    json7.as[AddressSource] shouldBe Cutover

    val json8 = Json.parse("""8""")
    json8.as[AddressSource] shouldBe RealTimeInformation

    val json9 = Json.parse("""9""")
    json9.as[AddressSource] shouldBe PersonalAccountUser
  }

  for {
    (addressSource, expectedJson) <- Seq(
                                       (NotKnown, Json.parse("""0""")),
                                       (Customer, Json.parse("""1""")),
                                       (Relative, Json.parse("""2""")),
                                       (Employer, Json.parse("""3""")),
                                       (InlandRevenue, Json.parse("""4""")),
                                       (OtherGovernmentDepartment, Json.parse("""5""")),
                                       (OtherThirdParty, Json.parse("""6""")),
                                       (Cutover, Json.parse("""7""")),
                                       (RealTimeInformation, Json.parse("""8""")),
                                       (PersonalAccountUser, Json.parse("""9"""))
                                     )
  }
    s"AddressSource $addressSource" should s"be written correctly to JSON" in {
      Json.toJson(addressSource.asInstanceOf[models.individualdetails.AddressSource]) shouldBe expectedJson
    }
}
