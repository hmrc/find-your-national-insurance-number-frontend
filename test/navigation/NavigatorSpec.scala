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
import controllers.routes
import models.HaveSetUpGGUserID.{No, Yes}
import models._
import pages._
import play.api.libs.json.Format.GenericFormat
import play.api.mvc.Call
import uk.gov.hmrc.http.HttpVerbs.GET

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator
  private val getNINOByPost = "/fill-online/get-your-national-insurance-number-by-post"

  "Navigator" - {

    "When normalRoutes" - {
      "for any other page" - {
        "must always go to IndexController" in {
          val page = mock[Page] // replace with a specific instance of Page
          val userAnswers = UserAnswers("id") // replace with a specific instance of UserAnswers
          navigator.nextPage(page, NormalMode, userAnswers) mustBe routes.IndexController.onPageLoad
        }
      }
    }

    "When on HaveSetUpGGUserIDPage" - {

      "when the answer is No" - {

        "must go to SetUpGGUserIDStartController" in {
          val userAnswers = UserAnswers("id").set(HaveSetUpGGUserIDPage, No).success.value
          navigator.nextPage(HaveSetUpGGUserIDPage, NormalMode, userAnswers) mustBe routes.SetUpGGUserIDStartController.onPageLoad()
        }
      }

      "when the answer is Yes" - {

        "must redirect to SMN" in {
          val userAnswers = UserAnswers("id").set(HaveSetUpGGUserIDPage, Yes).success.value
          navigator.nextPage(HaveSetUpGGUserIDPage, NormalMode, userAnswers) mustBe controllers.auth.routes.AuthController.redirectToSMN
        }
      }

      "when there is no answer" - {

        "must go to SetUpGGUserIDStartController" in {
          val userAnswers = UserAnswers("id")
          navigator.nextPage(HaveSetUpGGUserIDPage, NormalMode, userAnswers) mustBe routes.SetUpGGUserIDStartController.onPageLoad()
        }
      }
    }

    "When on InvalidDataNINOHelpPage" - {

      "when the answer is PhoneHmrc" - {

        "must go to PhoneHMRCDetailsController" in {
          val userAnswers = UserAnswers("id").set(InvalidDataNINOHelpPage, InvalidDataNINOHelp.PhoneHmrc).success.value
          navigator.nextPage(InvalidDataNINOHelpPage, NormalMode, userAnswers) mustBe routes.PhoneHMRCDetailsController.onPageLoad()
        }
      }

      "when the answer is PrintForm" - {

        "must go to the print and post service" in {
          val userAnswers = UserAnswers("id").set(InvalidDataNINOHelpPage, InvalidDataNINOHelp.PrintForm).success.value
          navigator.nextPage(InvalidDataNINOHelpPage, NormalMode, userAnswers) mustBe Call(GET, s"${config.printAndPostServiceUrl}$getNINOByPost")
        }
      }

      "when there is no answer" - {

        "must go to JourneyRecoveryController" in {
          val userAnswers = UserAnswers("id")
          navigator.nextPage(InvalidDataNINOHelpPage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "When on TechnicalErrorPage" - {

      "must go to JourneyRecoveryController" in {
        val userAnswers = UserAnswers("id")
        navigator.nextPage(TechnicalErrorPage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

    }

    "When on SelectAlternativeServicePage" - {

      "when the answer is PhoneHmrc" - {

        "must go to PhoneHMRCDetailsController" in {
          val userAnswers = UserAnswers("id").set(SelectAlternativeServicePage, SelectAlternativeService.PhoneHmrc).success.value
          navigator.nextPage(SelectAlternativeServicePage, NormalMode, userAnswers) mustBe routes.PhoneHMRCDetailsController.onPageLoad()
        }
      }

      "when the answer is PrintForm" - {

        "must go to the print and post service" in {
          val userAnswers = UserAnswers("id").set(SelectAlternativeServicePage, SelectAlternativeService.PrintForm).success.value
          navigator.nextPage(SelectAlternativeServicePage, NormalMode, userAnswers) mustBe Call(GET, s"${config.printAndPostServiceUrl}$getNINOByPost")
        }
      }

      "when there is no answer" - {

        "must go to JourneyRecoveryController" in {
          val userAnswers = UserAnswers("id")
          navigator.nextPage(SelectAlternativeServicePage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "When on SelectNINOLetterAddressPage" - {

      "when the answer is Postcode" - {

        "must go to NINOLetterPostedConfirmationController" in {
          val userAnswers = UserAnswers("id").set(SelectNINOLetterAddressPage, SelectNINOLetterAddress.Postcode).success.value
          navigator.nextPage(SelectNINOLetterAddressPage, NormalMode, userAnswers) mustBe routes.NINOLetterPostedConfirmationController.onPageLoad()
        }
      }

      "when the answer is NotThisAddress" - {

        "must go to SelectAlternativeServiceController" in {
          val userAnswers = UserAnswers("id").set(SelectNINOLetterAddressPage, SelectNINOLetterAddress.NotThisAddress).success.value
          navigator.nextPage(SelectNINOLetterAddressPage, NormalMode, userAnswers) mustBe routes.SelectAlternativeServiceController.onPageLoad(mode = NormalMode)
        }
      }

      "when there is no answer" - {

        "must go to JourneyRecoveryController" in {
          val userAnswers = UserAnswers("id")
          navigator.nextPage(SelectNINOLetterAddressPage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "When on ValidDataNINOMatchedNINOHelpPage" - {

      "when the answer is true" - {
        "must go to ConfirmYourPostcodeController" in {
          val userAnswers = UserAnswers("id").set(ValidDataNINOMatchedNINOHelpPage, true).success.value
          navigator.nextPage(ValidDataNINOMatchedNINOHelpPage, NormalMode, userAnswers) mustBe routes.ConfirmYourPostcodeController.onPageLoad(mode = NormalMode)
        }
      }

      "when the answer is false" - {
        "must go to SelectAlternativeServiceController" in {
          val userAnswers = UserAnswers("id").set(ValidDataNINOMatchedNINOHelpPage, false).success.value
          navigator.nextPage(ValidDataNINOMatchedNINOHelpPage, NormalMode, userAnswers) mustBe routes.SelectAlternativeServiceController.onPageLoad(mode = NormalMode)
        }
      }

      "when there is no answer" - {
        "must go to JourneyRecoveryController" in {
          val userAnswers = UserAnswers("id")
          navigator.nextPage(ValidDataNINOMatchedNINOHelpPage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "When on ValidDataNINOHelpPage" - {

      "when the answer is true" - {
        "must go to SelectNINOLetterAddressController" in {
          val userAnswers = UserAnswers("id").set(ValidDataNINOHelpPage, true).success.value
          navigator.nextPage(ValidDataNINOHelpPage, NormalMode, userAnswers) mustBe routes.SelectNINOLetterAddressController.onPageLoad(NormalMode)
        }
      }

      "when the answer is false" - {
        "must go to SelectAlternativeServiceController" in {
          val userAnswers = UserAnswers("id").set(ValidDataNINOHelpPage, false).success.value
          navigator.nextPage(ValidDataNINOHelpPage, NormalMode, userAnswers) mustBe routes.SelectAlternativeServiceController.onPageLoad(NormalMode)
        }
      }

      "when there is no answer" - {
        "must go to JourneyRecoveryController" in {
          val userAnswers = UserAnswers("id")
          navigator.nextPage(ValidDataNINOHelpPage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "When on EnteredPostCodeNotFoundPage" - {
      "when the answer is PhoneHmrc" - {
        "must go to PhoneHMRCDetailsController" in {
          val userAnswers = UserAnswers("id").set(EnteredPostCodeNotFoundPage, EnteredPostCodeNotFound.PhoneHmrc).success.value
          navigator.nextPage(EnteredPostCodeNotFoundPage, NormalMode, userAnswers) mustBe routes.PhoneHMRCDetailsController.onPageLoad()
        }

        "when the answer is PrintForm" - {

          "must go to the print and post service" in {
            val userAnswers = UserAnswers("id").set(EnteredPostCodeNotFoundPage, EnteredPostCodeNotFound.PrintForm).success.value
            navigator.nextPage(EnteredPostCodeNotFoundPage, NormalMode, userAnswers) mustBe Call(GET, s"${config.printAndPostServiceUrl}$getNINOByPost")
          }
        }

        "when there is no answer" - {

          "must go to JourneyRecoveryController" in {
            val userAnswers = UserAnswers("id")
            navigator.nextPage(EnteredPostCodeNotFoundPage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
          }
        }

      }


    }
  }
}
