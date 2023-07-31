package repositories

import com.google.inject.{Inject, Singleton}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Format, Json}
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.{MongoBinaryFormats, MongoJodaFormats}

import scala.concurrent.{ExecutionContext, Future}

case class RowPersonalDetailsValidation(validationId: String,
                                        validationStatus: String,
                                        personalDetails: String,
                                        dateCreated: String,
                                        lastUpdated: String)

object RowPersonalDetailsValidation {
  def apply(validationId: String,
            validationStatus: String,
            personalDetails: String,
            dateCreated: String): RowPersonalDetailsValidation = {
    RowPersonalDetailsValidation(validationId, validationStatus, personalDetails, dateCreated, DateTime.now(DateTimeZone.UTC).toLocalDateTime.toString())
  }

  implicit val dateFormat: Format[DateTime] = MongoJodaFormats.dateTimeFormat
  implicit val arrayFormat: Format[Array[Byte]] = MongoBinaryFormats.byteArrayFormat
  implicit val mongoFormat: Format[RowPersonalDetailsValidation] = Json.format[RowPersonalDetailsValidation]
}

@Singleton
class PersonalDetailsValidationRepository @Inject()(mongoComponent: MongoComponent)
                                                   (implicit ec: ExecutionContext) extends PlayMongoRepository[RowPersonalDetailsValidation](
  collectionName = "personal-details-validation",
  mongoComponent = mongoComponent,
  domainFormat = RowPersonalDetailsValidation.mongoFormat,
  indexes = Seq(
    IndexModel(
      Indexes.ascending("validationId"),
      IndexOptions().name("validationId").unique(true)
    ),
    IndexModel(
      Indexes.ascending("nino"),
      IndexOptions().name("nino").unique(true)
    )
  )
) with Logging {
  def insert(validationId: String,
             validationStatus: String,
             personalDetails: String,
             dateCreated: String)
            (implicit ec: ExecutionContext): Future[Unit] = {
    logger.info(s"Inserted one in $collectionName table")
    collection.insertOne(RowPersonalDetailsValidation(validationId, validationStatus, personalDetails, dateCreated, DateTime.now(DateTimeZone.UTC).
      toLocalDateTime.toString()))
      .toFuture().map(_ => ())
  }

  def findByValidationId(id: String)(implicit ec: ExecutionContext): Future[Option[RowPersonalDetailsValidation]] =
    collection.find(Filters.equal("validationId", id))
      .headOption()

  def findByNino(nino: String)(implicit ec: ExecutionContext): Future[Option[RowPersonalDetailsValidation]] =
    collection.find(Filters.equal("nino", nino))
      .headOption()
}
