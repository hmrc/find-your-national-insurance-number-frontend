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

package controllers

import controllers.actions.{IdentifierAction, PDVDataRequiredAction, PDVDataRetrievalAction}
import models.Mode
import models.pdv._
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{AuditService, IndividualDetailsService}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.FMNConstants.{FMNOrigin, IVOrigin, PDVOrigin}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckDetailsController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: IdentifierAction,
                                        pdvDataRetrievalAction: PDVDataRetrievalAction,
                                        pdvDataRequiredAction: PDVDataRequiredAction,
                                        auditService: AuditService,
                                        individualDetailsService: IndividualDetailsService,
                                        pdvResponseHandler: PDVResponseHandler,
                                        val controllerComponents: MessagesControllerComponents,
                                        val authConnector: AuthConnector
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with AuthorisedFunctions with I18nSupport with Logging {

  def onPageLoad(origin: Option[String], mode: Mode): Action[AnyContent] =
    (identify andThen pdvDataRetrievalAction andThen pdvDataRequiredAction).async {
      implicit request: PDVDataRequestWithUserAnswers[AnyContent] =>
        auditService.start()
        origin.map(_.toUpperCase) match {
          case Some(PDVOrigin) | Some(IVOrigin) | Some(FMNOrigin) =>
                for {
                  updatedAnswers <- individualDetailsService.cacheOrigin(request.userAnswers, origin)
                  result <- pdvResponseHandler.handlePDVResponse(request.pdvResponse, origin, updatedAnswers, mode)
                } yield result
          case _ =>
            logger.error(s"Invalid origin: $origin")
            Future.successful(Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode)))
        }
    }

}
