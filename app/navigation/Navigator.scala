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

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import models.HaveSetUpGGUserID.{No, Yes}
import models.ServiceIvEvidence
import pages._
import models._
import controllers._

@Singleton
class Navigator @Inject()() {

  private val normalRoutes: Page => UserAnswers => Call = {
    case HaveSetUpGGUserIDPage        => userAnswers => navigateHaveSetUpGGUserID(userAnswers)
    case PostNINOLetterPage           => userAnswers => navigatePostNINOLetterController(userAnswers)
    case SelectNINOLetterAddressPage  => userAnswers => navigateSelectNINOLetterAddressController(userAnswers)
    case ServiceIvEvidencePage    => userAnswers => navigateServiceIvEvidence(userAnswers)
    case ServiceIvIdPage          => userAnswers => navigateServiceIvId(userAnswers)
    case ServiceIvAppPage         => userAnswers => navigateServiceIvApp(userAnswers)
    case _                        => _ => routes.IndexController.onPageLoad
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
      case Some(No) => routes.SetUpGGUserIDStartController.onPageLoad()
      case Some(Yes) => routes.SetUpGGUserIDStartController.onPageLoad()
      case _ => routes.SetUpGGUserIDStartController.onPageLoad()
    }

  private def navigatePostNINOLetterController(userAnswers: UserAnswers): Call =
    userAnswers.get(PostNINOLetterPage) match {
      case Some(true) => routes.SelectNINOLetterAddressController.onPageLoad(mode = NormalMode)
      case Some(false) => routes.IndexController.onPageLoad
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateSelectNINOLetterAddressController(userAnswers: UserAnswers): Call =
    userAnswers.get(SelectNINOLetterAddressPage) match {
      case Some(SelectNINOLetterAddress.Postcode) => routes.NINOLetterPostedConfirmationController.onPageLoad()
      case Some(SelectNINOLetterAddress.NotThisAddress) => routes.IndexController.onPageLoad
      case _ => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateServiceIvEvidence(userAnswers: UserAnswers): Call =
    userAnswers.get(ServiceIvEvidencePage) match {
      case Some(ServiceIvEvidence.No) => controllers.routes.ServiceIvIdController.onPageLoad(NormalMode)
      case Some(ServiceIvEvidence.Yes) => controllers.routes.ServiceIvEvidenceController.onPageLoad(NormalMode)
      case _ => controllers.routes.ServiceIvEvidenceController.onPageLoad(NormalMode)
    }

  private def navigateServiceIvId(userAnswers: UserAnswers): Call =
    userAnswers.get(ServiceIvIdPage) match {
      case Some(false) => controllers.routes.ServiceIvIdController.onPageLoad(NormalMode)
      case Some(true) => controllers.routes.ServiceIvAppController.onPageLoad(NormalMode)
      case _ => controllers.routes.ServiceIvIdController.onPageLoad(NormalMode)
    }

  private def navigateServiceIvApp(userAnswers: UserAnswers): Call =
    userAnswers.get(ServiceIvIdPage) match {
      case Some(false) => controllers.routes.ServiceIvAppController.onPageLoad(NormalMode)
      case Some(true) => controllers.routes.ServiceIvAppController.onPageLoad(NormalMode)
      case _ => controllers.routes.ServiceIvAppController.onPageLoad(NormalMode)
    }
}
