package repositories

import config.FrontendAppConfig
import models.{TryAgainCount, UserAnswers}
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class TryAgainCountRepositoryISpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[TryAgainCount]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val tryAgainCount = TryAgainCount("id", 1, Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1

  protected override val repository = new TryAgainCountRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock
  )

  ".insertOrIncrement" - {

    "must insert count if no count exists when retry is first attempted" in {

      val expectedResult = tryAgainCount copy (lastUpdated = instant)

      val setResult = repository.insertOrIncrement(tryAgainCount.id).futureValue

      val updatedRecord = repository.findById(tryAgainCount.id).futureValue

      setResult mustEqual true
      updatedRecord.value.count mustEqual expectedResult.count
    }

    "must update count if count exists when retry is repeatedly attempted" in {

      val expectedResult = tryAgainCount copy (lastUpdated = instant, count = 2)

      repository.insertOrIncrement(tryAgainCount.id).futureValue

      val setResult = repository.insertOrIncrement(tryAgainCount.id).futureValue

      val updatedRecord = repository.findById(tryAgainCount.id).futureValue

      setResult mustEqual true
      updatedRecord.value.count mustEqual expectedResult.count
    }
  }

  ".findById" - {

    "when there is a record for this id" - {

      "must get the record" in {

        insert(tryAgainCount).futureValue

        val result         = repository.findById(tryAgainCount.id).futureValue
        val expectedResult = tryAgainCount

        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.findById("id that does not exist").futureValue must not be defined
      }
    }
  }
}
