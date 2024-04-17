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

package services

import connectors.PersonalDetailsValidationConnector
import models.pdv.{PDVNotFoundResponse, PDVRequest, PDVResponseData, PDVSuccessResponse, PersonalDetails}
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.mockito.MockitoSugar.mock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import repositories.{EncryptedPersonalDetailsValidationRepository, PersonalDetailsValidationRepository}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.{Instant, LocalDate}
import scala.concurrent.Future
import scala.util.Random

class PDVResponseDataServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach{

  import PDVResponseDataServiceSpec._
  implicit val mockDataRequest: DataRequest[AnyContent]  = mock[DataRequest[AnyContent]]

  override def beforeEach(): Unit = {
    reset(mockConnector, mockEncryptedPersonalDetailsValidationRepository, mockPersonalDetailsValidationRepository)
  }

  "PDVResponseDataService with EncryptedPersonalDetailsValidationRepository" must {

    val personalDetailsValidationService = new PersonalDetailsValidationService(
      mockConnector,
      mockEncryptedPersonalDetailsValidationRepository)(ec)

    "getPersonalDetailsValidationByNino" must {
      "return the details when nino exists" in {
        when(mockEncryptedPersonalDetailsValidationRepository.findByNino(eqTo(fakeNino.nino))(any()))
          .thenReturn(Future.successful(Option(personalDetailsValidation)))
        personalDetailsValidationService.getPersonalDetailsValidationByNino(fakeNino.nino).map { result =>
          result mustBe Some(personalDetailsValidation)
        }(ec)
      }
      "return None when nino does NOT exist" in {
        when(mockEncryptedPersonalDetailsValidationRepository.findByNino(eqTo("test2"))(any()))
          .thenReturn(Future.successful(None))

        personalDetailsValidationService.getPersonalDetailsValidationByNino("test2").map { result =>
          result mustBe None
        }(ec)
      }
    }

    "updatePDVDataRowWithValidationStatus" must {
      "update the row with valid validation status" in {
        when(mockEncryptedPersonalDetailsValidationRepository.updateCustomerValidityWithReason(any(), any(), any())(any()))
          .thenReturn(Future.successful(validationId))

        personalDetailsValidationService.updatePDVDataRowWithValidCustomer(validationId, isValidCustomer = true, "success").map { result =>
          result mustBe true
        }(ec)
      }
    }

    "updatePDVDataRowWithNPSPostCode" must {
      "update the row with valid NPS post code" in {
        when(mockEncryptedPersonalDetailsValidationRepository.updatePDVDataWithNPSPostCode(any(), any())(any()))
          .thenReturn(Future.successful(validationId))

        personalDetailsValidationService.updatePDVDataRowWithNPSPostCode(validationId, "AA1 1AA").map { result =>
          result mustBe true
        }(ec)
      }
    }

    "getValidCustomerStatus" must {
      "return true when valid customer status exists" in {
        when(mockEncryptedPersonalDetailsValidationRepository.findByNino(any())(any()))
          .thenReturn(Future.successful(Option(personalDetailsValidation2)))

        personalDetailsValidationService.getValidCustomerStatus(validationId).map { result =>
          result mustBe "true"
        }(ec)
      }
      "return None when valid customer status does NOT exist" in {
        when(mockEncryptedPersonalDetailsValidationRepository.findByNino(any())(any()))
          .thenReturn(Future.successful(None))

        personalDetailsValidationService.getValidCustomerStatus(validationId).map { result =>
          result mustBe "false"
        }(ec)
      }
    }

    "createPDVDataRow" must {
      "create a row with valid data" in {
        when(mockEncryptedPersonalDetailsValidationRepository.insertOrReplacePDVResultData(any())(any()))
          .thenReturn(Future.successful(validationId))

        personalDetailsValidationService.createPDVDataRow(pdvSuccessResponse).map { result =>
          result mustBe pdvSuccessResponse
        }(ec)
      }

      "not create a row with PdvNotFoundResponse status" in {
        val pdvNotFoundResponse = PDVNotFoundResponse(HttpResponse(404, "PDV data not found"))

        personalDetailsValidationService.createPDVDataRow(pdvNotFoundResponse).map { result =>
          result mustBe pdvNotFoundResponse
        }(ec)
      }
    }

    "getPDVMatchResult" must {
      "return true when the PDV data row has a valid customer status" in {
        when(mockEncryptedPersonalDetailsValidationRepository.findByNino(any())(any()))
          .thenReturn(Future.successful(Option(personalDetailsValidation2)))

        when(mockConnector.retrieveMatchingDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, Json.toJson(personalDetailsValidation2).toString())))

        personalDetailsValidationService.getPDVMatchResult(pdvRequest).map { result =>
          result.asInstanceOf[PDVSuccessResponse].leftSideValue.pdvResponseData.npsPostCode mustBe personalDetailsValidation2.npsPostCode
          result.asInstanceOf[PDVSuccessResponse].leftSideValue.pdvResponseData.validationStatus mustBe personalDetailsValidation2.validationStatus
        }(ec)
      }

      "return false when the PDV data row does NOT have a valid customer status" in {
        when(mockEncryptedPersonalDetailsValidationRepository.findByNino(any())(any()))
          .thenReturn(Future.successful(Option(personalDetailsValidation)))

        when(mockConnector.retrieveMatchingDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, Json.toJson(personalDetailsValidation).toString())))

        personalDetailsValidationService.getPDVMatchResult(pdvRequest).map { result =>
          result.asInstanceOf[PDVSuccessResponse].leftSideValue.pdvResponseData.npsPostCode mustBe personalDetailsValidation.npsPostCode
          result.asInstanceOf[PDVSuccessResponse].leftSideValue.pdvResponseData.validationStatus mustBe personalDetailsValidation.validationStatus
        }(ec)
      }

      "createPDVDataRow should reformat the postCode in PersonalDetails" in {
        val mockEncryptedRepository = mock[EncryptedPersonalDetailsValidationRepository]
        val mockRepository = mock[PersonalDetailsValidationRepository]
        val service = new PersonalDetailsValidationService(null, mockEncryptedRepository)(ec)

        val originalPostCode = "Ab 12C d"
        val expectedPostCode = "AB1 2CD" // assuming this is the expected format after splitPostCode

        val personalDetails = PersonalDetails(
          firstName = "John",
          lastName = "Doe",
          nino = Nino("AB123456C"),
          postCode = Some(originalPostCode),
          dateOfBirth = LocalDate.parse("1980-01-01")
        )

        val pdvResponseData: PDVResponseData = PDVResponseData(
          id = "abcd01234",
          validationStatus = "success",
          personalDetails = Some(personalDetails),
          reason = None,
          validCustomer = None,
          CRN = None,
          npsPostCode = None
        )

        when(mockRepository.insertOrReplacePDVResultData(any())(any()))
          .thenReturn(Future.successful("AB1 2CD"))

        service.createPDVDataRow(PDVSuccessResponse(pdvResponseData)).map {
          case PDVSuccessResponse(pdvResponseData) =>
            pdvResponseData.personalDetails mustBe defined
            pdvResponseData.personalDetails.get.postCode mustBe Some(expectedPostCode)
          case _ => fail("Expected PDVSuccessResponse")
        }
      }
    }

    "getPDVData" must {

      "return PDVResponseData when personalDetailsValidationService returns a successful response" ignore {
        val mockPDVRequest = PDVRequest("1234567890", "1234567890")

        val response = HttpResponse(200, Json.toJson(pdvSuccessResponse).toString())

        when(mockConnector.retrieveMatchingDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(response))

        when(personalDetailsValidationService.getPDVMatchResult(mockPDVRequest)(hc, mockDataRequest))
          .thenReturn(Future.successful(pdvSuccessResponse))
        when(personalDetailsValidationService.createPDVDataFromPDVMatch(mockPDVRequest)(hc, mockDataRequest))
          .thenReturn(Future.successful(pdvSuccessResponse))

        val result = personalDetailsValidationService.getPDVData(mockPDVRequest)

        result.map { pdvResponseData =>
          pdvResponseData mustBe pdvSuccessResponse
        }
      }

      "throw an exception when personalDetailsValidationService returns an error" in {
        val mockPDVRequest = PDVRequest("1234567890", "1234567890")

        when(mockConnector.retrieveMatchingDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, Json.toJson(personalDetailsValidation).toString())))

        when(personalDetailsValidationService.createPDVDataFromPDVMatch(mockPDVRequest))
          .thenReturn(Future.failed(new RuntimeException("Failed to get PDV data")))

        val result = personalDetailsValidationService.getPDVData(mockPDVRequest)

        result.failed.map { ex =>
          ex.getMessage mustBe "Failed to get PDV data"
        }
      }

    }

  }

  "PDVResponseDataService with PersonalDetailsValidationRepository" must {

    val personalDetailsValidationService = new PersonalDetailsValidationService(
      mockConnector,
      mockPersonalDetailsValidationRepository)(ec)

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

    "updatePDVDataRowWithValidationStatus" must {
      "update the row with valid validation status" in {
        when(mockPersonalDetailsValidationRepository.updateCustomerValidityWithReason(any(), any(), any())(any()))
          .thenReturn(Future.successful(validationId))

        personalDetailsValidationService.updatePDVDataRowWithValidCustomer(validationId, isValidCustomer = true, "success").map { result =>
          result mustBe true
        }(ec)
      }
    }

    "updatePDVDataRowWithNPSPostCode" must {
      "update the row with valid NPS post code" in {
        when(mockPersonalDetailsValidationRepository.updatePDVDataWithNPSPostCode(any(), any())(any()))
          .thenReturn(Future.successful(validationId))

        personalDetailsValidationService.updatePDVDataRowWithNPSPostCode(validationId, "AA1 1AA").map { result =>
          result mustBe true
        }(ec)
      }
    }

    "getValidCustomerStatus" must {
      "return true when valid customer status exists" in {
        when(mockPersonalDetailsValidationRepository.findByNino(any())(any()))
          .thenReturn(Future.successful(Option(personalDetailsValidation2)))

        personalDetailsValidationService.getValidCustomerStatus(validationId).map { result =>
          result mustBe "true"
        }(ec)
      }
      "return None when valid customer status does NOT exist" in {
        when(mockPersonalDetailsValidationRepository.findByNino(any())(any()))
          .thenReturn(Future.successful(None))

        personalDetailsValidationService.getValidCustomerStatus(validationId).map { result =>
          result mustBe "false"
        }(ec)
      }
    }

    "createPDVDataRow" must {
      "create a row with valid data" in {
        when(mockPersonalDetailsValidationRepository.insertOrReplacePDVResultData(any())(any()))
          .thenReturn(Future.successful(validationId))

        personalDetailsValidationService.createPDVDataRow(pdvSuccessResponse).map { result =>
          result mustBe pdvSuccessResponse
        }(ec)
      }

      "not create a row with PdvNotFoundResponse status" in {
        val pdvNotFoundResponse = PDVNotFoundResponse(HttpResponse(404, "PDV data not found"))

        personalDetailsValidationService.createPDVDataRow(pdvNotFoundResponse).map { result =>
          result mustBe pdvNotFoundResponse
        }(ec)
      }

    }

    "getPDVMatchResult" must {
      "return true when the PDV data row has a valid customer status" in {
        when(mockPersonalDetailsValidationRepository.findByNino(any())(any()))
          .thenReturn(Future.successful(Option(personalDetailsValidation2)))

        when(mockConnector.retrieveMatchingDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, Json.toJson(personalDetailsValidation2).toString())))

        personalDetailsValidationService.getPDVMatchResult(pdvRequest).map { result =>
          result.asInstanceOf[PDVSuccessResponse].leftSideValue.pdvResponseData.npsPostCode mustBe personalDetailsValidation2.npsPostCode
          result.asInstanceOf[PDVSuccessResponse].leftSideValue.pdvResponseData.validationStatus mustBe personalDetailsValidation2.validationStatus
        }(ec)
      }

      "return false when the PDV data row does NOT have a valid customer status" in {
        when(mockPersonalDetailsValidationRepository.findByNino(any())(any()))
          .thenReturn(Future.successful(Option(personalDetailsValidation)))

        when(mockConnector.retrieveMatchingDetails(any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, Json.toJson(personalDetailsValidation).toString())))

        personalDetailsValidationService.getPDVMatchResult(pdvRequest).map { result =>
          result.asInstanceOf[PDVSuccessResponse].leftSideValue.pdvResponseData.npsPostCode mustBe personalDetailsValidation.npsPostCode
          result.asInstanceOf[PDVSuccessResponse].leftSideValue.pdvResponseData.validationStatus mustBe personalDetailsValidation.validationStatus
        }(ec)
      }

      "createPDVDataRow should reformat the postCode in PersonalDetails" in {
        val mockPersonalDetailsValidationRepository = mock[PersonalDetailsValidationRepository]
        val mockRepository = mock[PersonalDetailsValidationRepository]
        val service = new PersonalDetailsValidationService(null, mockPersonalDetailsValidationRepository)(ec)

        val originalPostCode = "Ab 12C d"
        val expectedPostCode = "AB1 2CD" // assuming this is the expected format after splitPostCode

        val personalDetails = PersonalDetails(
          firstName = "John",
          lastName = "Doe",
          nino = Nino("AB123456C"),
          postCode = Some(originalPostCode),
          dateOfBirth = LocalDate.parse("1980-01-01")
        )

        val pdvResponseData: PDVResponseData = PDVResponseData(
          id = "abcd01234",
          validationStatus = "success",
          personalDetails = Some(personalDetails),
          reason = None,
          validCustomer = None,
          CRN = None,
          npsPostCode = None
        )

        when(mockRepository.insertOrReplacePDVResultData(any())(any()))
          .thenReturn(Future.successful("AB1 2CD"))

        service.createPDVDataRow(PDVSuccessResponse(pdvResponseData)).map {
          case PDVSuccessResponse(pdvResponseData) =>
            pdvResponseData.personalDetails mustBe defined
            pdvResponseData.personalDetails.get.postCode mustBe Some(expectedPostCode)
          case _ => fail("Expected PDVSuccessResponse")
        }
      }
    }

  }

}

object PDVResponseDataServiceSpec {
  private val mockConnector = mock[PersonalDetailsValidationConnector]
  private val mockEncryptedPersonalDetailsValidationRepository = mock[EncryptedPersonalDetailsValidationRepository]
  private val mockPersonalDetailsValidationRepository = mock[PersonalDetailsValidationRepository]

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val validationId = "abcd01234"
  val fakeNino: Nino = Nino(new Generator(new Random()).nextNino.nino)

  val pdvRequest: PDVRequest = PDVRequest("credid=12345", "session-67890")

  val personalDetails: PersonalDetails =
    PersonalDetails(
      "firstName",
      "lastName",
      fakeNino,
      Some("AA1 1AA"),
      LocalDate.parse("1945-03-18")
    )

  val pdvSuccessResponse: PDVSuccessResponse = PDVSuccessResponse(PDVResponseData(
      validationId,
      "success",
      Some(personalDetails),
      reason = None,
      validCustomer = None,
      CRN = None,
      npsPostCode = None
    ))

  val personalDetailsValidation: PDVResponseData = PDVResponseData(
      validationId,
      "success",
      Some(personalDetails),
      reason = None,
      validCustomer = None,
      CRN = None,
      npsPostCode = None
    )

  val personalDetailsValidation2: PDVResponseData =
    PDVResponseData(
      validationId,
      "success",
      Some(personalDetails),
      reason = None,
      validCustomer = Some("true"),
      CRN = None,
      npsPostCode = None,
      lastUpdated = Instant.ofEpochSecond(1234567890)
    )

}