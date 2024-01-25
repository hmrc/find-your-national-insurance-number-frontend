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

import base.SpecBase
import models.pdv.{PDVResponseData, PersonalDetails}
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.MockitoSugar.when
import pages.ValidDataNINOHelpPage
import play.api.inject
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repositories.SessionRepository
import services.PersonalDetailsValidationService
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, CredentialRole, User}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class ValidDataNINOHelpControllerSpec extends SpecBase {

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockSessionRepository: SessionRepository = mock[SessionRepository]
  val mockPersonalDetailsValidationService: PersonalDetailsValidationService = mock[PersonalDetailsValidationService]

  val userAnswers: UserAnswers = UserAnswers("id").set(ValidDataNINOHelpPage, true).success.value

  val userAnswersJson: JsValue = Json.toJson(userAnswers)

  val fakeRetrievalResult: Future[Option[CredentialRole] ~ Option[String]] =
    Future.successful(new ~(Some(User), Some("id")))

  val fakePDVResponseData: PDVResponseData = PDVResponseData(
    id = "fakeId",
    validationStatus = "success",
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

  "ValidDataNINOHelpController" - {

    "must return OK and the correct view for a GET" in {

      when(mockAuthConnector.authorise[Option[CredentialRole] ~ Option[String]](
        any[Predicate],
        any[Retrieval[Option[CredentialRole] ~ Option[String]]])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(fakeRetrievalResult)

      when(mockPersonalDetailsValidationService.getValidCustomerStatus(any[String]))
        .thenReturn(Future.successful("true"))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[AuthConnector].toInstance(mockAuthConnector),
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        ).build()

      running(application) {

        val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          Helpers.GET,
          controllers.routes.ValidDataNINOHelpController.onPageLoad(NormalMode).url
        ).withSession(
          SessionKeys.sessionId -> "id",
          "UserAnswers" -> userAnswersJson.toString(),
          "nino" -> "AA000003B"
        )

        val result = route(application, fakeRequest).value
        status(result) mustEqual OK
      }
    }

    "must redirect to UnauthorisedController when the user is not logged in" in {

      when(mockAuthConnector.authorise[Option[CredentialRole] ~ Option[String]](
        any[Predicate],
        any[Retrieval[Option[CredentialRole] ~ Option[String]]])(any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(fakeRetrievalResult)

      when(mockPersonalDetailsValidationService.getValidCustomerStatus(any[String]))
        .thenReturn(Future.successful("false"))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[AuthConnector].toInstance(mockAuthConnector),
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService)
        ).build()

      running(application) {
        val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          Helpers.GET,
          controllers.routes.ValidDataNINOHelpController.onPageLoad(NormalMode).url
        ).withSession(
          SessionKeys.sessionId -> "id",
          "UserAnswers" -> userAnswersJson.toString())

        val result = route(application, fakeRequest).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.UnauthorisedController.onPageLoad.url
      }
    }

    "must redirect to the next page for a POST with valid data" in {

        when(mockAuthConnector.authorise[Option[CredentialRole] ~ Option[String]](
          any[Predicate],
          any[Retrieval[Option[CredentialRole] ~ Option[String]]])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(fakeRetrievalResult)

        when(mockPersonalDetailsValidationService.getValidCustomerStatus(any[String]))
          .thenReturn(Future.successful("true"))

        when(mockPersonalDetailsValidationService.getPersonalDetailsValidationByNino(any[String]))
          .thenReturn(Future.successful(Some(fakePDVResponseData)))
        when(mockSessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          inject.bind[AuthConnector].toInstance(mockAuthConnector),
          inject.bind[SessionRepository].toInstance(mockSessionRepository),
          inject.bind[PersonalDetailsValidationService].toInstance(mockPersonalDetailsValidationService))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.ValidDataNINOHelpController.onSubmit(NormalMode).url).withFormUrlEncodedBody(("value", "true"))
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SelectNINOLetterAddressController.onPageLoad(NormalMode).url
      }

    }

  }
}