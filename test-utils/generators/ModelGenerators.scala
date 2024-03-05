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

package generators

import models._
import org.scalacheck.{Arbitrary, Gen}

trait ModelGenerators {

  implicit lazy val arbitraryOnlineOrLetter: Arbitrary[OnlineOrLetter] =
    Arbitrary {
      Gen.oneOf(OnlineOrLetter.values.toSeq)
    }

  implicit lazy val arbitraryEnteredPostCodeNotFound: Arbitrary[EnteredPostCodeNotFound] =
    Arbitrary {
      Gen.oneOf(EnteredPostCodeNotFound.values.toSeq)
    }

  implicit lazy val arbitrarySelectAlternativeService: Arbitrary[SelectAlternativeService] =
    Arbitrary {
      Gen.oneOf(SelectAlternativeService.values.toSeq)
    }

  implicit lazy val arbitraryInvalidDataNINOHelp: Arbitrary[InvalidDataNINOHelp] =
    Arbitrary {
      Gen.oneOf(InvalidDataNINOHelp.values.toSeq)
    }

  implicit lazy val arbitrarySelectNINOLetterAddress: Arbitrary[SelectNINOLetterAddress] =
    Arbitrary {
      Gen.oneOf(SelectNINOLetterAddress.values.toSeq)
    }

  implicit lazy val arbitraryHaveSetUpGGUserID: Arbitrary[HaveSetUpGGUserID] =
    Arbitrary {
      Gen.oneOf(HaveSetUpGGUserID.values.toSeq)
    }
}
