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

package navigation

import base.SpecBase
import controllers.{auth, routes}
import models._
import pages._
import play.api.libs.json.Format.GenericFormat
import play.api.mvc.Call
import uk.gov.hmrc.http.HttpVerbs.GET

class NavigatorSpec extends SpecBase {

  val navigator             = new Navigator
  private val getNINOByPost = "/fill-online/get-your-national-insurance-number-by-post"

  "Navigator" - {

    "When normalRoutes" - {
      "for any other page" - {
        "must always go to journey recovery" in {
          val page        = mock[Page] // replace with a specific instance of Page
          val userAnswers = UserAnswers() // replace with a specific instance of UserAnswers
          navigator.nextPage(page, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "when on ConfirmIdentityPage" - {
      "when the answer is yes" in {
        val userAnswers = UserAnswers().set(ConfirmIdentityPage, true).success.value
        navigator.nextPage(ConfirmIdentityPage, NormalMode, userAnswers) mustBe auth.routes.AuthController
          .redirectToSMN()
      }

      "when the answer is no" in {
        val userAnswers = UserAnswers().set(ConfirmIdentityPage, false).success.value
        navigator.nextPage(ConfirmIdentityPage, NormalMode, userAnswers) mustBe routes.PostLetterController.onPageLoad()
      }
    }

    "When on PostLetterPage" - {
      "when the answer is Yes" - {
        "must go to TracingWhatYouNeedController" in {
          val userAnswers = UserAnswers().set(PostLetterPage, true).success.value
          navigator.nextPage(PostLetterPage, NormalMode, userAnswers) mustBe routes.TracingWhatYouNeedController
            .onPageLoad()
        }
      }

      "when the answer is No" - {
        "must go to SelectAlternativeServiceController" in {
          val userAnswers = UserAnswers().set(PostLetterPage, false).success.value
          navigator.nextPage(PostLetterPage, NormalMode, userAnswers) mustBe routes.SelectAlternativeServiceController
            .onPageLoad()
        }
      }
    }

    "When on InvalidDataNINOHelpPage" - {

      "when the answer is PhoneHmrc" - {

        "must go to PhoneHMRCDetailsController" in {
          val userAnswers = UserAnswers().set(InvalidDataNINOHelpPage, InvalidDataNINOHelp.PhoneHmrc).success.value
          navigator.nextPage(InvalidDataNINOHelpPage, NormalMode, userAnswers) mustBe routes.PhoneHMRCDetailsController
            .onPageLoad()
        }
      }

      "when the answer is PrintForm" - {

        "must go to the print and post service" in {
          val userAnswers = UserAnswers().set(InvalidDataNINOHelpPage, InvalidDataNINOHelp.PrintForm).success.value
          navigator.nextPage(InvalidDataNINOHelpPage, NormalMode, userAnswers) mustBe Call(
            GET,
            s"${config.printAndPostServiceUrl}$getNINOByPost"
          )
        }
      }

      "when there is no answer" - {

        "must go to JourneyRecoveryController" in {
          val userAnswers = UserAnswers()
          navigator.nextPage(InvalidDataNINOHelpPage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController
            .onPageLoad()
        }
      }
    }

    "When on LetterTechnicalErrorPage" - {

      "must go to JourneyRecoveryController" in {
        val userAnswers = UserAnswers()
        navigator.nextPage(LetterTechnicalErrorPage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController
          .onPageLoad()
      }

    }

    "When on SelectAlternativeServicePage" - {

      "when the answer is PhoneHmrc" - {

        "must go to PhoneHMRCDetailsController" in {
          val userAnswers =
            UserAnswers().set(SelectAlternativeServicePage, SelectAlternativeService.PhoneHmrc).success.value
          navigator.nextPage(
            SelectAlternativeServicePage,
            NormalMode,
            userAnswers
          ) mustBe routes.PhoneHMRCDetailsController.onPageLoad()
        }
      }

      "when the answer is PrintForm" - {

        "must go to the print and post service" in {
          val userAnswers =
            UserAnswers().set(SelectAlternativeServicePage, SelectAlternativeService.PrintForm).success.value
          navigator.nextPage(SelectAlternativeServicePage, NormalMode, userAnswers) mustBe Call(
            GET,
            s"${config.printAndPostServiceUrl}$getNINOByPost"
          )
        }
      }

      "when there is no answer" - {

        "must go to JourneyRecoveryController" in {
          val userAnswers = UserAnswers()
          navigator.nextPage(
            SelectAlternativeServicePage,
            NormalMode,
            userAnswers
          ) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "When on SelectNINOLetterAddressPage" - {

      "when the answer is yes" - {

        "must go to NINOLetterPostedConfirmationController" in {
          val userAnswers = UserAnswers().set(SelectNINOLetterAddressPage, true).success.value
          navigator.nextPage(
            SelectNINOLetterAddressPage,
            NormalMode,
            userAnswers
          ) mustBe routes.NINOLetterPostedConfirmationController.onPageLoad()
        }
      }

      "when the answer is no" - {

        "must go to SelectAlternativeServiceController" in {
          val userAnswers = UserAnswers().set(SelectNINOLetterAddressPage, false).success.value
          navigator.nextPage(
            SelectNINOLetterAddressPage,
            NormalMode,
            userAnswers
          ) mustBe routes.SelectAlternativeServiceController.onPageLoad(mode = NormalMode)
        }
      }

      "when there is no answer" - {

        "must go to JourneyRecoveryController" in {
          val userAnswers = UserAnswers()
          navigator.nextPage(
            SelectNINOLetterAddressPage,
            NormalMode,
            userAnswers
          ) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "When on ValidDataNINOMatchedNINOHelpPage" - {

      "when the answer is true" - {
        "must go to ConfirmYourPostcodeController" in {
          val userAnswers = UserAnswers().set(ValidDataNINOMatchedNINOHelpPage, true).success.value
          navigator.nextPage(
            ValidDataNINOMatchedNINOHelpPage,
            NormalMode,
            userAnswers
          ) mustBe routes.ConfirmYourPostcodeController.onPageLoad(mode = NormalMode)
        }
      }

      "when the answer is false" - {
        "must go to SelectAlternativeServiceController" in {
          val userAnswers = UserAnswers().set(ValidDataNINOMatchedNINOHelpPage, false).success.value
          navigator.nextPage(
            ValidDataNINOMatchedNINOHelpPage,
            NormalMode,
            userAnswers
          ) mustBe routes.SelectAlternativeServiceController.onPageLoad(mode = NormalMode)
        }
      }

      "when there is no answer" - {
        "must go to JourneyRecoveryController" in {
          val userAnswers = UserAnswers()
          navigator.nextPage(
            ValidDataNINOMatchedNINOHelpPage,
            NormalMode,
            userAnswers
          ) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "When on ValidDataNINOHelpPage" - {

      "when the answer is true" - {
        "must go to SelectNINOLetterAddressController" in {
          val userAnswers = UserAnswers().set(ValidDataNINOHelpPage, true).success.value
          navigator.nextPage(
            ValidDataNINOHelpPage,
            NormalMode,
            userAnswers
          ) mustBe routes.SelectNINOLetterAddressController.onPageLoad(NormalMode)
        }
      }

      "when the answer is false" - {
        "must go to SelectAlternativeServiceController" in {
          val userAnswers = UserAnswers().set(ValidDataNINOHelpPage, false).success.value
          navigator.nextPage(
            ValidDataNINOHelpPage,
            NormalMode,
            userAnswers
          ) mustBe routes.SelectAlternativeServiceController.onPageLoad(NormalMode)
        }
      }

      "when there is no answer" - {
        "must go to JourneyRecoveryController" in {
          val userAnswers = UserAnswers()
          navigator.nextPage(ValidDataNINOHelpPage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController
            .onPageLoad()
        }
      }
    }

    "When on EnteredPostCodeNotFoundPage" - {
      "when the answer is PhoneHmrc" - {
        "must go to PhoneHMRCDetailsController" in {
          val userAnswers =
            UserAnswers().set(EnteredPostCodeNotFoundPage, EnteredPostCodeNotFound.PhoneHmrc).success.value
          navigator.nextPage(
            EnteredPostCodeNotFoundPage,
            NormalMode,
            userAnswers
          ) mustBe routes.PhoneHMRCDetailsController.onPageLoad()
        }

        "when the answer is PrintForm" - {

          "must go to the print and post service" in {
            val userAnswers =
              UserAnswers().set(EnteredPostCodeNotFoundPage, EnteredPostCodeNotFound.PrintForm).success.value
            navigator.nextPage(EnteredPostCodeNotFoundPage, NormalMode, userAnswers) mustBe Call(
              GET,
              s"${config.printAndPostServiceUrl}$getNINOByPost"
            )
          }
        }

        "when there is no answer" - {

          "must go to JourneyRecoveryController" in {
            val userAnswers = UserAnswers()
            navigator.nextPage(
              EnteredPostCodeNotFoundPage,
              NormalMode,
              userAnswers
            ) mustBe routes.JourneyRecoveryController.onPageLoad()
          }
        }

      }
    }
  }
}
