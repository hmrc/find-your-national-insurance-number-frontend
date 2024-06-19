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

package controllers.auth

import base.SpecBase
import controllers.bindable.Origin
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import scala.concurrent.Future

class AuthControllerSpec extends SpecBase with MockitoSugar {

  trait LocalSetup {
    val controller: AuthController = app.injector.instanceOf[AuthController]
  }

  "AuthController" - {

    "signOut" - {

      "must clear user answers and redirect to sign out, specifying the exit survey as the continue URL" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(None)
            .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

        running(application) {

          val sentLocation = "http://example.com&origin=FIND_MY_NINO"
          val request = FakeRequest(GET, routes.AuthController.signout(Some(RedirectUrl(sentLocation)), Some(Origin("FIND_MY_NINO"))).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

        }
      }

      "must not redirect when origin is missing" in {

        val mockSessionRepository = mock[SessionRepository]
        when(mockSessionRepository.clear(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(None)
            .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
            .build()

        running(application) {

          val sentLocation = "http://example.com&origin=FIND_MY_NINO"
          val request = FakeRequest(GET, routes.AuthController.signout(Some(RedirectUrl(sentLocation)), None).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
        }
      }
    }

    "timeOut" - {

      "return SEE_OTHER" in new LocalSetup {

        val result: Future[Result] = controller.timeOut()(FakeRequest("GET", ""))

        status(result) mustBe SEE_OTHER

      }

      "redirect to the session timeout page" in new LocalSetup {

        val result: Future[Result] = controller.timeOut()(FakeRequest("GET", ""))

        redirectLocation(result).getOrElse("Unable to complete") mustBe controllers.auth.routes.SignedOutController.onPageLoad.url
      }

      "clear the session upon redirect" in new LocalSetup {

        val result: Future[Result] = controller.timeOut()(FakeRequest("GET", "").withSession("test" -> "session"))

        session(result) mustBe empty
      }
    }

    "redirectToRegister" - {

      "must redirect to the register page" in new LocalSetup {
        
        val redirectUrl: Option[RedirectUrl] = Some(RedirectUrl("http://localhost:9553/feedback-survey?origin=FIND_MY_NINO"))

        val result: Future[Result] = controller.redirectToRegister(redirectUrl)(FakeRequest("GET", ""))

        status(result) mustBe SEE_OTHER
        val expectedResult = "http://localhost:9553/bas-gateway/register?origin=find-your-national-insurance-number-frontend&continueUrl=http%3A%2F%2Flocalhost%3A9553%2Ffeedback-survey%3Forigin%3DFIND_MY_NINO&accountType=Individual"
        redirectLocation(result).value mustBe expectedResult
      }

    }

    "redirectToSMN" - {

      "must redirect to the SMN page" in new LocalSetup {

        val result: Future[Result] = controller.redirectToSMN()(FakeRequest("GET", ""))

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe "http://localhost:9949/auth-login-stub/gg-sign-in?continue=http%3A%2F%2Flocalhost"
      }
    }

  }

}
