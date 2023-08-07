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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PersonalDetailsValidationService
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class CheckDetailsControllerSpec extends SpecBase with SummaryListFluency {

  "CheckDetailsController" - {

    val validationId = "31423424242423r23g4resds"
    val mockPersonalDetailsValidationService =  mock[PersonalDetailsValidationService]

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      when(mockPersonalDetailsValidationService.createPDVFromValidationId(any())(any()))
        .thenReturn(Future.successful(validationId))

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(validationId).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]
        val list = SummaryListViewModel(Seq.empty)

        status(result) mustEqual SEE_OTHER
        contentAsString(result) contains view(list)(request, messages(application)).toString
      }
    }

  }
}
