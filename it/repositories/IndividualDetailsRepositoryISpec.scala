

package repositories

import config.FrontendAppConfig
import models.encryption.id.EncryptedIndividualDetailsDataCache
import models.individualdetails.{IndividualDetailsData, IndividualDetailsDataCache}
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.RecoverMethods.recoverToExceptionIf
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logging
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
class IndividualDetailsRepositoryISpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[IndividualDetailsDataCache]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar with Logging {


  val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1

  protected override val repository = new IndividualDetailsRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
  )

  private val individualDetailsData = IndividualDetailsDataCache(
    "session-1f84da66-d49a-40d3-ad72-9142acb90000",
    Some(IndividualDetailsData("John", "Doe", "1980-01-01", "AB12CD", "AB123456C"))
  )

  "IndividualDetailsRepository" - {

    ".insertOrReplaceIndividualDetailsData" - {

      "when there is a record for this id" - {

        "must update the record" in {

          repository.insertOrReplaceIndividualDetailsData(individualDetailsData).futureValue
          val result = repository.findIndividualDetailsDataByNino(individualDetailsData.getNino).futureValue
          result.value.copy(lastUpdated = Instant.EPOCH) mustEqual individualDetailsData.copy(lastUpdated = Instant.EPOCH)
        }
      }
    }

    ".findIndividualDetailsDataByNino" - {

      "when there is a record for this nino" - {

        "must get the record" in {

          repository.insertOrReplaceIndividualDetailsData(individualDetailsData).futureValue
          val result = repository.findIndividualDetailsDataByNino(individualDetailsData.getNino).futureValue
          result.value.copy(lastUpdated = Instant.EPOCH) mustEqual individualDetailsData.copy(lastUpdated = Instant.EPOCH)
        }
      }
    }


    "when an exception is thrown" - {

      "must log an error and return a failed future" in {

        val nonExistentNino = "ZZ999999Z"

        recoverToExceptionIf[Exception] {
          repository.findIndividualDetailsDataByNino(nonExistentNino)
        } map { ex =>
          ex.getMessage must include("Failed finding Individual Details Data by Nino")
        }
      }
    }


  }



}