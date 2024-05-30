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
import controllers.auth
import models.ServiceIv._
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
        "must always go to journey recovery" in {
          val page = mock[Page] // replace with a specific instance of Page
          val userAnswers = UserAnswers("id") // replace with a specific instance of UserAnswers
          navigator.nextPage(page, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
        }
      }
    }

    "when extended IV journey is toggled off" - {
      "when on ConfirmIdentityPage" - {
        "when the answer is yes" in {
          val userAnswers = UserAnswers("id").set(ConfirmIdentityPage, true).success.value
          navigator.nextPage(ConfirmIdentityPage, NormalMode, userAnswers) mustBe auth.routes.AuthController.redirectToSMN
        }

        "when the answer is no" in {
          val userAnswers = UserAnswers("id").set(ConfirmIdentityPage, false).success.value
          navigator.nextPage(ConfirmIdentityPage, NormalMode, userAnswers) mustBe routes.PostLetterController.onPageLoad()
        }
      }
    }

    "when extended IV journey is toggled on" - {
      "When on ServiceIvPage" - {
        "for NoneOfTheAbove" - {
          "must go to the PostLetterController" in {
            val value: Set[ServiceIv] = Set(NoneOfTheAbove)
            val userAnswers = UserAnswers("id").set(ServiceIvPage, value).success.value
            navigator.nextPage(ServiceIvPage, NormalMode, userAnswers) mustBe routes.PostLetterController.onPageLoad()
          }
        }

        "for a single item that is compatible with the app" - {
          "for UkPhotocardDrivingLicence only" - {
            "must go to the ServiceIvAppController" in {
              val value: Set[ServiceIv] = Set(UkPhotocardDrivingLicence)
              val userAnswers = UserAnswers("id").set(ServiceIvPage, value).success.value
              navigator.nextPage(ServiceIvPage, NormalMode, userAnswers) mustBe routes.ServiceIvAppController.onPageLoad()
            }
          }

          "for ValidUkPassport only" - {
            "must go to the ServiceIvAppController" in {
              val value: Set[ServiceIv] = Set(ValidUkPassport)
              val userAnswers = UserAnswers("id").set(ServiceIvPage, value).success.value
              navigator.nextPage(ServiceIvPage, NormalMode, userAnswers) mustBe routes.ServiceIvAppController.onPageLoad()
            }
          }

          "for NonUkPassport only" - {
            "must go to the ServiceIvAppController" in {
              val value: Set[ServiceIv] = Set(NonUkPassport)
              val userAnswers = UserAnswers("id").set(ServiceIvPage, value).success.value
              navigator.nextPage(ServiceIvPage, NormalMode, userAnswers) mustBe routes.ServiceIvAppController.onPageLoad()
            }
          }

          "for UkBiometricResidenceCard only" - {
            "must go to the ServiceIvAppController" in {
              val value: Set[ServiceIv] = Set(UkBiometricResidenceCard)
              val userAnswers = UserAnswers("id").set(ServiceIvPage, value).success.value
              navigator.nextPage(ServiceIvPage, NormalMode, userAnswers) mustBe routes.ServiceIvAppController.onPageLoad()
            }
          }
        }

        "for more than one item selected (valid IV)" - {
          "must go to the AuthController" in {
            val value: Set[ServiceIv] = Set(UkPhotocardDrivingLicence, ValidUkPassport)
            val userAnswers = UserAnswers("id").set(ServiceIvPage, value).success.value
            navigator.nextPage(ServiceIvPage, NormalMode, userAnswers) mustBe auth.routes.AuthController.redirectToSMN
          }
        }

        "for a single items that are not compatible with the app" - {
          "for PayslipOrP60" - {
            "must go to the PostLetterController" in {
              val value: Set[ServiceIv] = Set(PayslipOrP60)
              val userAnswers = UserAnswers("id").set(ServiceIvPage, value).success.value
              navigator.nextPage(ServiceIvPage, NormalMode, userAnswers) mustBe routes.PostLetterController.onPageLoad()
            }
          }

          "for TaxCreditsClaim" - {
            "must go to the PostLetterController" in {
              val value: Set[ServiceIv] = Set(TaxCreditsClaim)
              val userAnswers = UserAnswers("id").set(ServiceIvPage, value).success.value
              navigator.nextPage(ServiceIvPage, NormalMode, userAnswers) mustBe routes.PostLetterController.onPageLoad()
            }
          }

          "for SelfAssessment" - {
            "must go to the PostLetterController" in {
              val value: Set[ServiceIv] = Set(SelfAssessment)
              val userAnswers = UserAnswers("id").set(ServiceIvPage, value).success.value
              navigator.nextPage(ServiceIvPage, NormalMode, userAnswers) mustBe routes.PostLetterController.onPageLoad()
            }
          }
        }

        "for no selections (catch all)" - {
          "must go to the SelectAlternativeServiceController" in {
            val userAnswers = UserAnswers("id")
            navigator.nextPage(ServiceIvPage, NormalMode, userAnswers) mustBe routes.SelectAlternativeServiceController.onPageLoad()
          }
        }
      }

      "When on ServiceIvAppPage" - {
        "when the answer is Yes" - {
          "must go to store" in {
            val userAnswers = UserAnswers("id").set(ServiceIvAppPage, true).success.value
            navigator.nextPage(ServiceIvAppPage, NormalMode, userAnswers) mustBe auth.routes.AuthController.redirectToSMN
          }
        }

        "when the answer is No" - {
          "must go to PostLetterController" in {
            val userAnswers = UserAnswers("id").set(ServiceIvAppPage, false).success.value
            navigator.nextPage(ServiceIvAppPage, NormalMode, userAnswers) mustBe routes.PostLetterController.onPageLoad()
          }
        }
      }
    }

    "When on PostLetterPage" - {
      "when the answer is Yes" - {
        "must go to TracingWhatYouNeedController" in {
          val userAnswers = UserAnswers("id").set(PostLetterPage, true).success.value
          navigator.nextPage(PostLetterPage, NormalMode, userAnswers) mustBe routes.TracingWhatYouNeedController.onPageLoad()
        }
      }

      "when the answer is No" - {
        "must go to SelectAlternativeServiceController" in {
          val userAnswers = UserAnswers("id").set(PostLetterPage, false).success.value
          navigator.nextPage(PostLetterPage, NormalMode, userAnswers) mustBe routes.SelectAlternativeServiceController.onPageLoad()
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

    "When on LetterTechnicalErrorPage" - {

      "must go to JourneyRecoveryController" in {
        val userAnswers = UserAnswers("id")
        navigator.nextPage(LetterTechnicalErrorPage, NormalMode, userAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
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

      "when the answer is yes" - {

        "must go to NINOLetterPostedConfirmationController" in {
          val userAnswers = UserAnswers("id").set(SelectNINOLetterAddressPage, true).success.value
          navigator.nextPage(SelectNINOLetterAddressPage, NormalMode, userAnswers) mustBe routes.NINOLetterPostedConfirmationController.onPageLoad()
        }
      }

      "when the answer is no" - {

        "must go to SelectAlternativeServiceController" in {
          val userAnswers = UserAnswers("id").set(SelectNINOLetterAddressPage, false).success.value
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
