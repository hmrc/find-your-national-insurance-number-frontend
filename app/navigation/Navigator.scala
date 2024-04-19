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

import config.FrontendAppConfig
import controllers.routes
import models.ServiceIv._
import models._
import pages._
import play.api.mvc.Call
import uk.gov.hmrc.http.HttpVerbs.GET

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject()(implicit config: FrontendAppConfig) {

  private lazy val getNINOByPost = "/fill-online/get-your-national-insurance-number-by-post"

  private val normalRoutes: Page => UserAnswers => Call = {
    case ServiceIvPage                      => userAnswers => navigateIvEvidence(userAnswers)
    case ServiceIvAppPage                   => userAnswers => navigateCanDownloadApp(userAnswers)
    case PostLetterPage                     => userAnswers => navigatePostLetter(userAnswers)
    case SelectNINOLetterAddressPage        => userAnswers => navigateSelectNINOLetterAddress(userAnswers)
    case SelectAlternativeServicePage       => userAnswers => navigateSelectAlternativeService(userAnswers)
    case LetterTechnicalErrorPage           => userAnswers => navigateLetterTechnicalError(userAnswers)
    case InvalidDataNINOHelpPage            => userAnswers => navigateInvalidDataNINOHelp(userAnswers)
    case ValidDataNINOHelpPage              => userAnswers => navigateValidDataNINOHelp(userAnswers)
    case EnteredPostCodeNotFoundPage        => userAnswers => navigateEnteredPostCodeNotFound(userAnswers)
    case ValidDataNINOMatchedNINOHelpPage   => userAnswers => navigateValidDataNINOMatchedNINOHelp(userAnswers)
    case _                                  => _           => routes.JourneyRecoveryController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case _ =>
      normalRoutes(page)(userAnswers)
  }

  private def navigatePostLetter(userAnswers: UserAnswers): Call =
    userAnswers.get(PostLetterPage) match {
      case Some(true) => controllers.routes.TracingWhatYouNeedController.onPageLoad()
      case _          => routes.SelectAlternativeServiceController.onPageLoad()
    }
  private def navigateCanDownloadApp(userAnswers: UserAnswers): Call =
    userAnswers.get(ServiceIvAppPage) match {
      case Some(true) => controllers.auth.routes.AuthController.redirectToSMN
      case _          => controllers.routes.PostLetterController.onPageLoad()
    }

  private def navigateIvEvidence(userAnswers: UserAnswers): Call =
    userAnswers.get(ServiceIvPage) match {
      case Some(selections) =>
        selections.toSeq match {
          case Seq(NoneOfTheAbove) => controllers.routes.PostLetterController.onPageLoad()
          case Seq(UkPhotocardDrivingLicence)
               | Seq(ValidUkPassport)
               | Seq(NonUkPassport)
               | Seq(UkBiometricResidenceCard) =>
            controllers.routes.ServiceIvAppController.onPageLoad()
          case _ => if (selections.toList.length > 1) {
            controllers.auth.routes.AuthController.redirectToSMN
          } else {
            controllers.routes.PostLetterController.onPageLoad()
          }
        }
      case _ => routes.SelectAlternativeServiceController.onPageLoad()
    }

  private def navigateSelectNINOLetterAddress(userAnswers: UserAnswers): Call =
    userAnswers.get(SelectNINOLetterAddressPage) match {
      case Some(SelectNINOLetterAddress.Postcode)       => routes.NINOLetterPostedConfirmationController.onPageLoad()
      case Some(SelectNINOLetterAddress.NotThisAddress) => routes.SelectAlternativeServiceController.onPageLoad(mode = NormalMode)
      case _                                            => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateSelectAlternativeService(userAnswers: UserAnswers): Call =
    userAnswers.get(SelectAlternativeServicePage) match {
      case Some(SelectAlternativeService.PhoneHmrc) => routes.PhoneHMRCDetailsController.onPageLoad()
      case Some(SelectAlternativeService.PrintForm) => Call(GET, s"${config.printAndPostServiceUrl}$getNINOByPost")
      case _                                        => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateLetterTechnicalError(userAnswers: UserAnswers): Call =
    userAnswers.get(LetterTechnicalErrorPage) match {
      case Some(LetterTechnicalError.PhoneHmrc) => routes.PhoneHMRCDetailsController.onPageLoad()
      case Some(LetterTechnicalError.PrintForm) => Call(GET, s"${config.printAndPostServiceUrl}$getNINOByPost")
      case _                                     => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateInvalidDataNINOHelp(userAnswers: UserAnswers): Call =
    userAnswers.get(InvalidDataNINOHelpPage) match {
      case Some(InvalidDataNINOHelp.PhoneHmrc) => routes.PhoneHMRCDetailsController.onPageLoad()
      case Some(InvalidDataNINOHelp.PrintForm) => Call(GET, s"${config.printAndPostServiceUrl}$getNINOByPost")
      case _                                   => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateValidDataNINOHelp(userAnswers: UserAnswers): Call =
    userAnswers.get(ValidDataNINOHelpPage) match {
      case Some(true) => routes.SelectNINOLetterAddressController.onPageLoad(mode = NormalMode)
      case Some(false) => routes.SelectAlternativeServiceController.onPageLoad(mode = NormalMode)
      case _                                 => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateEnteredPostCodeNotFound(userAnswers: UserAnswers): Call =
    userAnswers.get(EnteredPostCodeNotFoundPage) match {
      case Some(EnteredPostCodeNotFound.PhoneHmrc) => routes.PhoneHMRCDetailsController.onPageLoad()
      case Some(EnteredPostCodeNotFound.PrintForm) => Call(GET, s"${config.printAndPostServiceUrl}$getNINOByPost")
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
  
  private def navigateValidDataNINOMatchedNINOHelp(userAnswers: UserAnswers): Call =
    userAnswers.get(ValidDataNINOMatchedNINOHelpPage) match {
      case Some(true) => routes.ConfirmYourPostcodeController.onPageLoad(mode = NormalMode)
      case Some(false) => routes.SelectAlternativeServiceController.onPageLoad(mode = NormalMode)
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }

}
