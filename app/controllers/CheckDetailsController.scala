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

package controllers

import config.FrontendAppConfig
import controllers.actions.IdentifierAction
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PersonalDetailsValidationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class CheckDetailsController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: IdentifierAction,
                                        personalDetailsValidationService: PersonalDetailsValidationService,
                                        val controllerComponents: MessagesControllerComponents
                                      )(implicit ec: ExecutionContext, appConfig: FrontendAppConfig) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(validationId: String): Action[AnyContent] = identify {
    implicit request =>
      // TODO Step 1:- PDV Validation logic
      // TODO Step 2:- API 1694 integration

      personalDetailsValidationService.createPDVFromValidationId(validationId).onComplete {
        case Success(value) => {
          logger.info(value)
          personalDetailsValidationService.getPersonalDetailsValidationByValidationId(validationId).onComplete {
            case Success(pdv) => {
              logger.info("pdv result: " + pdv)
              val ninoFromPDV = pdv.map(_.personalDetails.map(_.nino).getOrElse("")).getOrElse("")
              val postCodeFromPDV = pdv.map(_.personalDetails.map(_.postCode).getOrElse("")).getOrElse("")
            }
            case Failure(ex) => logger.warn(ex.getMessage)
          }
        }
        case Failure(ex) => logger.warn(ex.getMessage)
      }

      val postCodeMatched = true // TODO expecting flag value after performing these two steps
      if(postCodeMatched) {
        Redirect(routes.ValidDataNINOHelpController.onPageLoad())
      } else {
        Redirect(routes.InvalidDataNINOHelpController.onPageLoad())
      }
  }

}