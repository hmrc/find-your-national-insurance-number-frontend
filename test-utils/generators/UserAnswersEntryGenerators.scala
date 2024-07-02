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
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import pages._
import play.api.libs.json.{JsValue, Json}

trait UserAnswersEntryGenerators extends PageGenerators with ModelGenerators {

  implicit lazy val arbitraryConfirmIdentityUserAnswersEntry: Arbitrary[(ConfirmIdentityPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ConfirmIdentityPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryPostLetterUserAnswersEntry: Arbitrary[(PostLetterPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[PostLetterPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }


  implicit lazy val arbitraryEnteredPostCodeNotFoundUserAnswersEntry: Arbitrary[(EnteredPostCodeNotFoundPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[EnteredPostCodeNotFoundPage.type]
        value <- arbitrary[EnteredPostCodeNotFound].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryValidDataNINOMatchedNINOHelpUserAnswersEntry: Arbitrary[(ValidDataNINOMatchedNINOHelpPage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ValidDataNINOMatchedNINOHelpPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryConfirmYourPostcodeUserAnswersEntry: Arbitrary[(ConfirmYourPostcodePage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[ConfirmYourPostcodePage.type]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitrarySelectAlternativeServiceUserAnswersEntry: Arbitrary[(SelectAlternativeServicePage.type, JsValue)] =
    Arbitrary {
      for {
        page  <- arbitrary[SelectAlternativeServicePage.type]
        value <- arbitrary[SelectAlternativeService].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitrarySelectNINOLetterAddressUserAnswersEntry: Arbitrary[(SelectNINOLetterAddressPage.type, JsValue)] =
  Arbitrary {
      for {
        page  <- arbitrary[SelectNINOLetterAddressPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryValidDataNINOHelpUserAnswersEntry: Arbitrary[(ValidDataNINOHelpPage.type, JsValue)] =
    Arbitrary {
      for {
        page <- Arbitrary.arbitrary[ValidDataNINOHelpPage.type]
        value <- Arbitrary.arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }
}
