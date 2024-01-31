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

package models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ModeSpec extends AnyFlatSpec with Matchers {

  "JavascriptLiteral for Mode" should "correctly convert NormalMode to string" in {
    val mode = NormalMode
    val result = Mode.jsLiteral.to(mode)
    result shouldBe "NormalMode"
  }

  it should "correctly convert CheckMode to string" in {
    val mode = CheckMode
    val result = Mode.jsLiteral.to(mode)
    result shouldBe "CheckMode"
  }

  "Mode bindable" should "correctly bind NormalMode from query parameters" in {
    val params = Map("mode" -> Seq("NormalMode"))
    val result = Mode.bindable.bind("mode", params)
    result shouldBe Some(Right(NormalMode))
  }

  it should "correctly bind CheckMode from query parameters" in {
    val params = Map("mode" -> Seq("CheckMode"))
    val result = Mode.bindable.bind("mode", params)
    result shouldBe Some(Right(CheckMode))
  }

  it should "return Left with error message for unknown mode" in {
    val params = Map("mode" -> Seq("UnknownMode"))
    val result = Mode.bindable.bind("mode", params)
    result shouldBe Some(Left("Unknown mode: UnknownMode"))
  }

  it should "return None if mode parameter is not present" in {
    val params = Map("other" -> Seq("value"))
    val result = Mode.bindable.bind("mode", params)
    result shouldBe None
  }

  "Mode bindable" should "correctly unbind NormalMode into a query parameter string" in {
    val result = Mode.bindable.unbind("mode", NormalMode)
    result shouldBe "mode=NormalMode"
  }

  it should "correctly unbind CheckMode into a query parameter string" in {
    val result = Mode.bindable.unbind("mode", CheckMode)
    result shouldBe "mode=CheckMode"
  }
}