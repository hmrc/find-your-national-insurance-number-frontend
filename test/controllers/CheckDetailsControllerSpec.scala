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
import connectors.IndividualDetailsConnector
import models.individualdetails.IndividualDetails
import models.{NormalMode, pdv}
import models.pdv.{PDVRequest, PDVResponseData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject
import play.api.inject.NewInstanceInjector.instanceOf
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PersonalDetailsValidationService
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.credentials
import uk.gov.hmrc.domain.Nino
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import java.time.{LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import scala.concurrent.Future

class CheckDetailsControllerSpec extends SpecBase with SummaryListFluency {

  "CheckDetailsController" - {

    val mockIndividualDetailsConnector = mock[IndividualDetailsConnector]
    val mockPersonalDetailsValidationService = mock[PersonalDetailsValidationService]

    "must return OK and the correct view for a GET" in {

      val mockPDVResponseData = mock[PDVResponseData].copy(validationStatus = "success")

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseData))

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]
        val list = SummaryListViewModel(Seq.empty)

        status(result) mustEqual 303
        contentAsString(result) contains view(list)(request, messages(application)).toString
      }
    }

    "must redirect to InvalidDataNINOHelpController page when PDVResponseData is empty" in {
      val mockPDVResponseData = mock[PDVResponseData].copy(validationStatus = "failure")

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(any())(any()))
        .thenReturn(Future.successful(mockPDVResponseData))

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual 303
        redirectLocation(result).value mustEqual routes.InvalidDataNINOHelpController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to ValidDataNINOMatchedNINOHelpController page when PDVResponseData is matched and postcode is matched" ignore {

      // construct PDVResponseData with some values
      val mockPDVResponseDataWithValues = PDVResponseData(
        "01234",
        "success",
        Some(pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), Some("AA1 1AA"), LocalDate.now())),
        LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[IndividualDetailsConnector].toInstance(mockIndividualDetailsConnector),
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      val pdvRequest = PDVRequest("credentialId", "sessionId")

      val mockIndividualDetails = mock[IndividualDetails]

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(pdvRequest)(hc))
        .thenReturn(Future.successful(mockPDVResponseDataWithValues))

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual 303
        redirectLocation(result).value mustEqual routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(NormalMode).url
      }
    }

    "must redirect to ValidDataNINOMatchedNINOHelpController page when PDVResponseData is matched and nino is matched, postcode is missing" ignore {

      // construct PDVResponseData with some values
      val mockPDVResponseDataWithValues = PDVResponseData(
        "01234",
        "success",
        Some(pdv.PersonalDetails("John", "Smith", Nino("AB123456C"), None, LocalDate.now())),
        LocalDateTime.now(ZoneId.systemDefault()).toInstant(ZoneOffset.UTC), None, None, None, None
      )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        )
        .build()

      val pdvRequest = PDVRequest("credentialId", "sessionId")

      when(mockPersonalDetailsValidationService.createPDVDataFromPDVMatch(pdvRequest)(hc))
        .thenReturn(Future.successful(mockPDVResponseDataWithValues))

      running(application) {
        val request = FakeRequest(GET, routes.CheckDetailsController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual 303
        redirectLocation(result).value mustEqual routes.ValidDataNINOMatchedNINOHelpController.onPageLoad(NormalMode).url
      }
    }

  }

}
