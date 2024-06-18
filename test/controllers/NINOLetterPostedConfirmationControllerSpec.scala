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

import base.SpecBase
import models.pdv.{PDVResponseData, PersonalDetails, ValidationStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{PersonalDetailsValidationService, SessionCacheService}
import uk.gov.hmrc.domain.Nino
import views.html.NINOLetterPostedConfirmationView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class NINOLetterPostedConfirmationControllerSpec extends SpecBase {

  lazy val ninoLetterPostedConfirmationRoute: String = routes.NINOLetterPostedConfirmationController.onPageLoad().url
  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]
  val mockSessionCacheService: SessionCacheService = mock[SessionCacheService]

  val fakePDVResponseData: PDVResponseData = PDVResponseData(
    id = "fakeId",
    validationStatus = ValidationStatus.Success,
    personalDetails = Some(PersonalDetails(
      firstName = "John",
      lastName = "Doe",
      nino = Nino("AA000003B"),
      postCode = Some("AA1 1AA"),
      dateOfBirth = LocalDate.of(1990, 1, 1)
    )),
    validCustomer = Some("true"),
    CRN = Some("fakeCRN"),
    npsPostCode = Some("AA1 1AA"),
    reason = None
  )

  override def beforeEach(): Unit = {
    reset(
      mockSessionCacheService
    )
  }

  val fakePDVResponseDataInvalidCustomer: PDVResponseData = fakePDVResponseData.copy(
    validCustomer = Some("false")
  )

  "NINOLetterPostedConfirmation Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseData)))

      when(mockSessionCacheService.invalidateCache(any(), any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          inject.bind[SessionCacheService].toInstance(mockSessionCacheService),
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.NINOLetterPostedConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NINOLetterPostedConfirmationView]

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces mustEqual view(LocalDate.now.format(DateTimeFormatter.ofPattern("d MMMM uuuu")))(request, messages).toString

        verify(mockSessionCacheService, times(1)).invalidateCache(any(), any())
      }
    }

    "must return OK and the correct view for a GET when cache invalidation fails" in {

      when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
        .thenReturn(Future.successful(Some(fakePDVResponseData)))

      when(mockSessionCacheService.invalidateCache(any(), any())).thenReturn(Future.successful(false))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService),
          inject.bind[SessionCacheService].toInstance(mockSessionCacheService),
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.NINOLetterPostedConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NINOLetterPostedConfirmationView]

        status(result) mustEqual OK
        contentAsString(result).removeAllNonces mustEqual view(LocalDate.now.format(DateTimeFormatter.ofPattern("d MMMM uuuu")))(request, messages).toString

        verify(mockSessionCacheService, times(1)).invalidateCache(any(), any())
      }
    }
  }
}
