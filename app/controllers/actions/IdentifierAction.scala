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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]

class SessionIdentifierAction @Inject()(
     override val authConnector: AuthConnector,
     config: FrontendAppConfig,
     val parser: BodyParsers.Default
 )(implicit val executionContext: ExecutionContext) extends IdentifierAction with AuthorisedFunctions with Logging{

  private val AuthPredicate = AuthProviders(GovernmentGateway)
  private val FMNRetrievals = Retrievals.credentials and Retrievals.confidenceLevel

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(AuthPredicate).retrieve(FMNRetrievals) {
      case Some(credentials) ~ confidenceLevel =>
        hc.sessionId match {
          case Some(session) =>
            if (confidenceLevel.level > ConfidenceLevel.L50.level) {
              Future.successful(Redirect(config.storeMyNinoUrl, Map("continue" -> Seq(resolveCorrectUrl(request)))))
            } else {
              block(IdentifierRequest(request, session.value, Some(credentials.providerId)))
            }
          case None =>
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      case _ =>
        throw new UnauthorizedException("Unable to retrieve internal Id and credentials Id.")
    } recover {
      case _: NoActiveSession =>
        logger.warn(s"[SessionIdentifierAction][invokeBlock] No active session, redirecting...")
        Redirect(config.loginUrl, Map("continue" -> Seq(resolveCorrectUrl(request))))
      case _: AuthorisationException =>
        logger.warn(s"[SessionIdentifierAction][invokeBlock] Authorisation exception, redirecting....}")
        Redirect(routes.UnauthorisedController.onPageLoad)
    }
   }

  private def resolveCorrectUrl[A](request: Request[A]): String = {
    val root =
      if (request.host.contains("localhost")) s"http${if (request.secure) "s" else ""}://${request.host}" else ""
    s"$root${request.uri}"
  }

}
