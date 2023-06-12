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

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.Stubs.{userLoggedInFMNUser, userLoggedInIsNotFMNUser}
import util.TestData.{NinoUser, NotLiveNinoUser}
import play.api.i18n.MessagesApi
import play.api.inject.NewInstanceInjector.instanceOf
import views.html.ViewNinoInPTAView

class ViewNinoInPTAControllerSpec extends SpecBase with MockitoSugar {

  lazy val controller = instanceOf[ViewNinoInPTAController]

  lazy implicit val messagesApi: MessagesApi = controller.messagesApi

  "ViewNinoInPTA Controller" - {

      "must return OK and the correct save your NINO view for a live NINO with GET request" in {
        userLoggedInFMNUser(NinoUser)

        val application = applicationBuilderWithConfig(userAnswers = Some(emptyUserAnswers)).build()


        running(application) {
          val request = FakeRequest(GET, routes.ViewNinoInPTAController.onPageLoad.url).withSession(("authToken", "Bearer 123"))

          val result = route(application, request).value

          val view = application.injector.instanceOf[ViewNinoInPTAView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view("http://localhost:14006/save-your-national-insurance-number")(request, messages(application)).toString
        }
      }

      "must return OK and the correct interruptPage view for a NOT live NINO with GET request" in {
        userLoggedInFMNUser(NotLiveNinoUser)

        val application = applicationBuilderWithConfig(userAnswers = Some(emptyUserAnswers)).build()


        running(application) {
          val request = FakeRequest(GET, routes.ViewNinoInPTAController.onPageLoad.url).withSession(("authToken", "Bearer 123"))

          val result = route(application, request).value

          val view = application.injector.instanceOf[ViewNinoInPTAView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view("/find-your-national-insurance-number-frontend/interruptPage")(request, messages(application)).toString
        }
      }

    "must return OK and the correct interruptPage view for a GET request" in {

      val application = applicationBuilderWithConfig(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ViewNinoInPTAController.interruptPage.url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentType(result) mustEqual Some("text/html")
      }
    }

    "must fail to login user" in {
      userLoggedInIsNotFMNUser(NinoUser)

      val application = applicationBuilderWithConfig(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ViewNinoInPTAController.onPageLoad.url).withSession(("authToken", "Bearer 123"))

        val result = route(application, request).value

        status(result) mustEqual 500
      }
    }

  }
}
