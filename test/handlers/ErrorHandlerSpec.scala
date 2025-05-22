/*
 * Copyright 2025 HM Revenue & Customs
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

package handlers

import base.SpecBase
import controllers.routes
import models.{NormalMode, OriginType}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import views.html.ErrorTemplate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ErrorHandlerSpec extends SpecBase {

  import ErrorHandlerSpec._

  "ErrorHandler" - {
    "must return the correct error page for a bad request" in {

      when(errorTemplate.apply(any[String], any[String], any[String])(any(), any()))
        .thenReturn(Html("<h1>test template</h1>"))

      val errorHandler = new ErrorHandler(messagesApi, errorTemplate)

      val result: Future[Html] =
        errorHandler.standardErrorTemplate("pageTitle", "bad request", "some message")(fakeRequest)

      result.futureValue mustEqual Html("<h1>test template</h1>")
    }
  }

}

object ErrorHandlerSpec {

  val messagesApi: MessagesApi                         = mock[MessagesApi]
  val errorTemplate: ErrorTemplate                     = mock[ErrorTemplate]
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, routes.CheckDetailsController.onPageLoad(Some(OriginType.PDV), NormalMode).url)
  implicit val fakeMessages: Messages                  = messagesApi.preferred(fakeRequest)
}
