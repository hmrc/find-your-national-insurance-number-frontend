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

package services

import connectors.PersonalDetailsValidationConnector
import models.{PersonalDetails, PDVResponseData, PDVSuccessResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.mockito.MockitoSugar.mock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import repositories.PersonalDetailsValidationRepository
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Random

class PDVResponseDataServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach{

  import PDVResponseDataServiceSpec._

  override def beforeEach(): Unit = {
    reset(mockConnector, mockPersonalDetailsValidationRepository)
  }

  "getPersonalDetailsValidationByValidationId" must {
    "return the details when validationId exists" in {

      when(mockPersonalDetailsValidationRepository.findByValidationId(eqTo(validationId))(any()))
        .thenReturn(Future.successful(Option(personalDetailsValidation)))
      personalDetailsValidationService.getPersonalDetailsValidationByValidationId(validationId).map { result =>
        result mustBe Some(personalDetailsValidation)
      }(ec)
    }
    "return None when validationId does NOT exist" in {
      when(mockPersonalDetailsValidationRepository.findByValidationId(eqTo("test2"))(any()))
        .thenReturn(Future.successful(None))

      personalDetailsValidationService.getPersonalDetailsValidationByValidationId("test2").map { result =>
        result mustBe None
      }(ec)
    }
  }

  "getPersonalDetailsValidationByNino" must {
    "return the details when nino exists" in {
      when(mockPersonalDetailsValidationRepository.findByNino(eqTo(fakeNino.nino))(any()))
        .thenReturn(Future.successful(Option(personalDetailsValidation)))
      personalDetailsValidationService.getPersonalDetailsValidationByNino(fakeNino.nino).map { result =>
        result mustBe Some(personalDetailsValidation)
      }(ec)
    }
    "return None when nino does NOT exist" in {
      when(mockPersonalDetailsValidationRepository.findByNino(eqTo("test2"))(any()))
        .thenReturn(Future.successful(None))

      personalDetailsValidationService.getPersonalDetailsValidationByNino("test2").map { result =>
        result mustBe None
      }(ec)
    }
  }

  "createPDVFromValidationId" must {
    "return success string when passed a valid validationId" in {
      when(mockConnector.retrieveMatchingDetails(any())(any(), any()))
        .thenReturn(Future(PDVSuccessResponse(personalDetailsValidation))(ec))

      when(mockPersonalDetailsValidationRepository.insert(any())(any()))
        .thenReturn(Future.successful(validationId))

      personalDetailsValidationService.createPDVDataFromPDVMatch(validationId)(hc).map { result =>
        result mustBe validationId
      }(ec)
    }
  }

}

object PDVResponseDataServiceSpec {
  private val mockConnector = mock[PersonalDetailsValidationConnector]
  private val mockPersonalDetailsValidationRepository = mock[PersonalDetailsValidationRepository]

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val personalDetailsValidationService = new PersonalDetailsValidationService(mockConnector, mockPersonalDetailsValidationRepository)

  val validationId = "abc1234"
  val fakeNino = Nino(new Generator(new Random()).nextNino.nino)

  val personalDetails: PersonalDetails =
    PersonalDetails(
      "firstName",
      "lastName",
      fakeNino,
      Some("AA1 1AA"),
      LocalDate.parse("1945-03-18")
    )
  val personalDetailsValidation: PDVResponseData =
    PDVResponseData(
      validationId,
      "success",
      Some(personalDetails)
    )

}