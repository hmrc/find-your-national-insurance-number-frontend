/*
 * Copyright 2023 HM Revenue & Customs
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
import models._
import pages._
import play.api.mvc.Call
import uk.gov.hmrc.http.HttpVerbs.GET

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject()(implicit config: FrontendAppConfig) {

  val getNINOByPost = "/fill-online/get-your-national-insurance-number-by-post"

  private val normalRoutes: Page => UserAnswers => Call = {
    case HaveSetUpGGUserIDPage        => userAnswers => navigateHaveSetUpGGUserID(userAnswers)
    case PostNINOLetterPage           => userAnswers => navigatePostNINOLetter(userAnswers)
    case SelectNINOLetterAddressPage  => userAnswers => navigateSelectNINOLetterAddress(userAnswers)
    case SelectAlternativeServicePage => userAnswers => navigateSelectAlternativeService(userAnswers)
    case TechnicalErrorPage           => userAnswers => navigateTechnicalErrorService(userAnswers)
    case InvalidDataNINOHelpPage      => userAnswers => navigateInvalidDataNINOHelp(userAnswers)
    case ValidDataNINOHelpPage        => userAnswers => navigateValidDataNINOHelp(userAnswers)
    case _                            => _           => routes.IndexController.onPageLoad
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case _ => _ => routes.CheckYourAnswersController.onPageLoad
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }

  private def navigateHaveSetUpGGUserID(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveSetUpGGUserIDPage) match {
      case Some(No)   => routes.SetUpGGUserIDStartController.onPageLoad()
      case Some(Yes)  => controllers.auth.routes.AuthController.redirectToSMN
      case _          => routes.SetUpGGUserIDStartController.onPageLoad()
    }

  private def navigatePostNINOLetter(userAnswers: UserAnswers): Call =
    userAnswers.get(PostNINOLetterPage) match {
      case Some(true)   => routes.SelectNINOLetterAddressController.onPageLoad(mode = NormalMode)
      case Some(false)  => routes.SelectAlternativeServiceController.onPageLoad(mode = NormalMode)
      case _            => routes.JourneyRecoveryController.onPageLoad()
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
      case Some(SelectAlternativeService.PrintForm) => Call(GET, s"${config.printAndPostServiceUrl}${getNINOByPost}")
      case _                                        => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateTechnicalErrorService(userAnswers: UserAnswers): Call =
    userAnswers.get(TechnicalErrorPage) match {
      case Some(TechnicalErrorService.TryAgain)  => routes.SelectNINOLetterAddressController.onPageLoad(mode = NormalMode)
      case Some(TechnicalErrorService.PhoneHmrc) => routes.PhoneHMRCDetailsController.onPageLoad()
      case Some(TechnicalErrorService.PrintForm) => Call(GET, s"${config.printAndPostServiceUrl}${getNINOByPost}")
      case _                                     => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateInvalidDataNINOHelp(userAnswers: UserAnswers): Call =
    userAnswers.get(InvalidDataNINOHelpPage) match {
      case Some(InvalidDataNINOHelp.PhoneHmrc) => routes.PhoneHMRCDetailsController.onPageLoad()
      case Some(InvalidDataNINOHelp.PrintForm) => Call(GET, s"${config.printAndPostServiceUrl}/fill-online/get-your-national-insurance-number-by-post")
      case _                                   => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateValidDataNINOHelp(userAnswers: UserAnswers): Call =
    userAnswers.get(ValidDataNINOHelpPage) match {
      case Some(ValidDataNINOHelp.OnlineService) => routes.SelectNINOLetterAddressController.onPageLoad(mode = NormalMode)
      case Some(ValidDataNINOHelp.PhoneHmrc) => routes.PhoneHMRCDetailsController.onPageLoad()
      case Some(ValidDataNINOHelp.PrintForm) => Call(GET, s"${config.printAndPostServiceUrl}/fill-online/get-your-national-insurance-number-by-post")
      case _                                 => routes.JourneyRecoveryController.onPageLoad()
    }

}
