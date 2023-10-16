package repositories

import config.FrontendAppConfig
import models.{PDVResponseData, PersonalDetails}
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Instant, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

class PDVResponseRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[PDVResponseData]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val fakeNino = Nino(new Generator(new Random()).nextNino.nino)

  val personalDetails: PersonalDetails =
    PersonalDetails(
      "firstName",
      "lastName",
      fakeNino,
      Some("AA1 1AA"),
      LocalDate.parse("1945-03-18")
    )

  private val pdvResponseData = PDVResponseData("id", "valid", Some(personalDetails), Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1

  protected override val repository = new PersonalDetailsValidationRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
  )

  ".findByValidationId" - {

    "when there is a record for this id" - {

      "must get the record" in {
        insert(pdvResponseData).futureValue

        val result = repository.findByValidationId(pdvResponseData.id).futureValue
        val expectedResult = pdvResponseData

        result.value mustEqual expectedResult
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
        insert(pdvResponseData).futureValue

        val result = repository.findByNino(pdvResponseData.personalDetails.get.nino.value).futureValue
        val expectedResult = pdvResponseData

        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {
        repository.findByValidationId("id that does not exist").futureValue must not be defined
      }
    }
  }
}
