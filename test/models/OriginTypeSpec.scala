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

import base.SpecBase

class OriginTypeSpec extends SpecBase {

  "toFeedbackSource" - {
    "must return correct source for each type" in {
      OriginType.toFeedbackSource(OriginType.FMN) mustBe "FIND_MY_NINO"
      OriginType.toFeedbackSource(OriginType.PDV) mustBe "PDV"
      OriginType.toFeedbackSource(OriginType.IV) mustBe "IV"
    }
  }

}
