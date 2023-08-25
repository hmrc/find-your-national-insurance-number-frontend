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

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import models.HaveSetUpGGUserID.{No, Yes}
import pages._
import models._
import uk.gov.hmrc.http.HttpVerbs.GET

@Singleton
class Navigator @Inject()(implicit config: FrontendAppConfig) {

  private val normalRoutes: Page => UserAnswers => Call = {
    case HaveSetUpGGUserIDPage        => userAnswers => navigateHaveSetUpGGUserID(userAnswers)
    case PostNINOLetterPage           => userAnswers => navigatePostNINOLetter(userAnswers)
    case SelectNINOLetterAddressPage  => userAnswers => navigateSelectNINOLetterAddress(userAnswers)
    case SelectAlternativeServicePage => userAnswers => navigateSelectAlternativeService(userAnswers)
    case NINOHelplinePage             => userAnswers => navigateNINOHelpline(userAnswers)
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
      case Some(SelectAlternativeService.PrintForm) => Call(GET, s"${config.printAndPostServiceUrl}/fill-online/get-your-national-insurance-number-by-post")
      case _                                        => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateNINOHelpline(userAnswers: UserAnswers): Call =
    userAnswers.get(NINOHelplinePage) match {
      case Some(NINOHelpline.OnlineService) => routes.SelectNINOLetterAddressController.onPageLoad(mode = NormalMode)
      case Some(NINOHelpline.PhoneHmrc) => routes.PhoneHMRCDetailsController.onPageLoad()
      case Some(NINOHelpline.PrintForm) => Call(GET, s"${config.printAndPostServiceUrl}/fill-online/get-your-national-insurance-number-by-post")
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }
}
