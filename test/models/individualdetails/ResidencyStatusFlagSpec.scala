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

import models.individualdetails.ResidencyStatusFlag.{UK, Abroad}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

class ResidencyStatusFlagSpec extends AnyFunSuite with Matchers {

  test("ResidencyStatusFlag reads/writes") {
    val ukFlag = UK
    val abroadFlag = Abroad

    val ukJson = Json.toJson(ukFlag.asInstanceOf[ResidencyStatusFlag])
    val abroadJson = Json.toJson(abroadFlag.asInstanceOf[ResidencyStatusFlag])

    ukJson.as[Int] shouldEqual 0
    abroadJson.as[Int] shouldEqual 1

    ukJson.as[ResidencyStatusFlag] shouldEqual ukFlag
    abroadJson.as[ResidencyStatusFlag] shouldEqual abroadFlag
  }
}