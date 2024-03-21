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
import models.HaveSetUpGGUserID.{No, Yes}
import models.UpliftOrLetter._
import models._
import pages._
import play.api.mvc.Call
import uk.gov.hmrc.http.HttpVerbs.GET
import util.FMNConstants.FMNOrigin

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject()(implicit config: FrontendAppConfig) {

  private lazy val origin        = FMNOrigin
  private lazy val redirectUrl   = config.fmnCheckDetailsUrl
  private lazy val getNINOByPost = "/fill-online/get-your-national-insurance-number-by-post"
  private lazy val pdvStart      = s"/personal-details-validation/start?completionUrl=$redirectUrl&origin=$origin&failureUrl=$redirectUrl"

  private val normalRoutes: Page => UserAnswers => Call = {
    case UpliftOrLetterPage                 => userAnswers => navigateCanIv(userAnswers)
    case ServiceIvAppPage                   => userAnswers => navigateOnlineOrLetter(userAnswers)
    case HaveSetUpGGUserIDPage              => userAnswers => navigateHaveSetUpGGUserID(userAnswers)
    case SelectNINOLetterAddressPage        => userAnswers => navigateSelectNINOLetterAddress(userAnswers)
    case SelectAlternativeServicePage       => userAnswers => navigateSelectAlternativeService(userAnswers)
    case LetterTechnicalErrorPage           => userAnswers => navigateLetterTechnicalError(userAnswers)
    case InvalidDataNINOHelpPage            => userAnswers => navigateInvalidDataNINOHelp(userAnswers)
    case ValidDataNINOHelpPage              => userAnswers => navigateValidDataNINOHelp(userAnswers)
    case EnteredPostCodeNotFoundPage        => userAnswers => navigateEnteredPostCodeNotFound(userAnswers)
    case ValidDataNINOMatchedNINOHelpPage   => userAnswers => navigateValidDataNINOMatchedNINOHelp(userAnswers)
    case _                                  => _           => routes.IndexController.onPageLoad
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case _ =>
      normalRoutes(page)(userAnswers)
  }

  private def navigateOnlineOrLetter(userAnswers: UserAnswers): Call =
    userAnswers.get(ServiceIvAppPage) match {
      case Some(true) => controllers.auth.routes.AuthController.redirectToSMN
      case _          => Call(GET, s"${config.personalDetailsValidationFrontEnd}$pdvStart")
    }

  private def navigateCanIv(userAnswers: UserAnswers): Call =
    userAnswers.get(UpliftOrLetterPage) match {
      case Some(selections) =>
        selections.toSeq match {
          case Seq(NoneOfTheAbove)            => Call(GET, s"${config.personalDetailsValidationFrontEnd}$pdvStart")
          case Seq(UkPhotocardDrivingLicence) => controllers.routes.ServiceIvAppController.onPageLoad()
          case Seq(ValidUkPassport)           => controllers.routes.ServiceIvAppController.onPageLoad()
          case _ => if (selections.toList.length > 1) {
            controllers.auth.routes.AuthController.redirectToSMN
          } else {
            Call(GET, s"${config.personalDetailsValidationFrontEnd}$pdvStart")
          }
        }
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateHaveSetUpGGUserID(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveSetUpGGUserIDPage) match {
      case Some(No)   => routes.SetUpGGUserIDStartController.onPageLoad()
      case Some(Yes)  => controllers.auth.routes.AuthController.redirectToSMN
      case _          => routes.SetUpGGUserIDStartController.onPageLoad()
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
