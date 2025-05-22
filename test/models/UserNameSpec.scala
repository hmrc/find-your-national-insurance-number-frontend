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
import uk.gov.hmrc.auth.core.retrieve.Name

class UserNameSpec extends AnyFlatSpec with Matchers {

  "An UserName" should "have name and lastname" in {
    val name     = Name(Some("first"), Some("last"))
    val userName = UserName(name)
    userName.name.name            should be(Some("first"))
    userName.name.lastName        should be(Some("last"))
    userName.toString             should be("first last")
    userName.getOrElse("default") should be("first last")
  }

}
