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

import base.SpecBase
import controllers.actions.IdentifierActionSpec.{fakeAuthConnector, retrievals200, retrievals50}
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class IdentifierActionSpec extends SpecBase {

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  "Auth Action" - {

    "when the user has a confidence level of 50" - {

      "will grant access" in {
        val application = applicationBuilder(userAnswers = None).build()

        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

        val authAction = new SessionIdentifierAction(fakeAuthConnector(retrievals50), config, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest.withSession("sessionId" -> "SomeSession"))

        status(result) mustBe OK
      }
    }

    "when the user has a confidence level > 50" - {

      "will redirect to store" in {
        val application = applicationBuilder(userAnswers = None).build()

        val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

        val authAction = new SessionIdentifierAction(fakeAuthConnector(retrievals200), config, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustEqual controllers.auth.routes.AuthController.redirectToSMN.url
      }
    }
  }
}

object IdentifierActionSpec {

  private def fakeAuthConnector(stubbedRetrievalResult: Future[_]) = new AuthConnector {

    def authorise[A](predicate: Predicate, retrieval: Retrieval[A])
                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      stubbedRetrievalResult.asInstanceOf[Future[A]]
    }
  }

  private def retrievals200: Future[Some[Credentials] ~ ConfidenceLevel.L200.type] = Future.successful(
    new ~ (Some(Credentials("gg", "cred-1234")), ConfidenceLevel.L200)
  )

  private def retrievals50: Future[Some[Credentials] ~ ConfidenceLevel.L50.type] = Future.successful(
    new~(Some(Credentials("gg", "cred-1234")), ConfidenceLevel.L50)
  )
}





