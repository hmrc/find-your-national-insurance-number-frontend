package repositories

import config.FrontendAppConfig
import models.pdv.{PDVResponseData, PersonalDetails}
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import repositories.encryption.EncryptedPDVResponseData
import repositories.encryption.EncryptedPDVResponseData.encrypt
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class PDVResponseRepositoryISpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EncryptedPDVResponseData]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val fakeNino = Nino(new Generator(new Random()).nextNino.nino)

  val personalDetails: PersonalDetails =
    PersonalDetails(
      "firstName",
      "lastName",
      fakeNino,
      Some("AA1 1AA"),
      LocalDate.parse("1945-03-18")
    )

  private val pdvResponseData = PDVResponseData(
    "id", "valid", Some(personalDetails), Instant.ofEpochSecond(1), None, Some("false"), Some("false"),Some("somePostcode"))

  private val validCustomerPDVResponseData = PDVResponseData(
    "id", "valid", Some(personalDetails), Instant.ofEpochSecond(1), Some("Valid Reason"), Some("true"), Some("false"),Some("somePostcode"))

  private val invalidCustomerPDVResponseData = PDVResponseData(
    "id", "valid", Some(personalDetails), Instant.ofEpochSecond(1), Some("Invalid Reason"), Some("false"), Some("false"),Some("somePostcode"))

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1
  when(mockAppConfig.encryptionKey) thenReturn "z4rWoRLf7a1OHTXLutSDJjhrUzZTBE3b"

  protected override val repository = new EncryptedPersonalDetailsValidationRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
  )


  ".updateCustomerValidityWithReason" - {

    "when there is a record for this id" - {

      "must update record to be a valid customer" in {

        val expectedResult = validCustomerPDVResponseData copy (lastUpdated = instant)

        insert(encrypt(pdvResponseData, mockAppConfig.encryptionKey)).futureValue

        repository.updateCustomerValidityWithReason(pdvResponseData.getNino, validCustomer = true, "Valid Reason").futureValue

        val result = repository.findByValidationId(pdvResponseData.id).futureValue

        result.value.validationStatus mustEqual expectedResult.validationStatus
        result.value.personalDetails  mustEqual expectedResult.personalDetails
        result.value.reason           mustEqual expectedResult.reason
      }

      "must update record to be an invalid customer" in {

        val expectedResult = invalidCustomerPDVResponseData copy (lastUpdated = instant)

        insert(encrypt(pdvResponseData, mockAppConfig.encryptionKey)).futureValue

        repository.updateCustomerValidityWithReason(pdvResponseData.getNino, validCustomer = false, "Invalid Reason").futureValue

        val result = repository.findByValidationId(pdvResponseData.id).futureValue

        result.value.validationStatus mustEqual expectedResult.validationStatus
        result.value.personalDetails  mustEqual expectedResult.personalDetails
        result.value.reason           mustEqual expectedResult.reason
      }
    }
  }

  ".findByValidationId" - {

    "when there is a record for this id" - {

      "must get the record" in {

        insert(encrypt(pdvResponseData, mockAppConfig.encryptionKey)).futureValue

        val result = repository.findByValidationId(pdvResponseData.id).futureValue
        val expectedResult = pdvResponseData

        result.value.id               mustEqual expectedResult.id
        result.value.personalDetails  mustEqual expectedResult.personalDetails
        result.value.validationStatus mustEqual expectedResult.validationStatus
      }
    }

    "when there is no record for this id" - {

      "must return None" in {
        repository.findByValidationId("id that does not exist").futureValue must not be defined
      }
    }
  }

  ".findByNino" - {

    "when there is a record for this id" - {

      "must get the record" in {

        insert(encrypt(pdvResponseData, mockAppConfig.encryptionKey)).futureValue

        val result = repository.findByNino(pdvResponseData.personalDetails.get.nino.value).futureValue
        val expectedResult = pdvResponseData

        result.value.id mustEqual expectedResult.id
        result.value.personalDetails mustEqual expectedResult.personalDetails
        result.value.validationStatus mustEqual expectedResult.validationStatus
      }
    }

    "when there is no record for this id" - {

      "must return None" in {
        repository.findByValidationId("id that does not exist").futureValue must not be defined
      }
    }
  }
}
