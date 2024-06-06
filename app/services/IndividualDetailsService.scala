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

import cacheables.OriginCacheable
import com.google.inject.ImplementedBy
import connectors.IndividualDetailsConnector
import models.IndividualDetailsResponseEnvelope.IndividualDetailsResponseEnvelope
import models.errors.{IndividualDetailsError, InvalidIdentifier}
import models.individualdetails.AddressType.ResidentialAddress
import models.{CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope, UserAnswers}
import models.individualdetails.{Address, AddressList, IndividualDetails, IndividualDetailsData, IndividualDetailsDataCache, ResolveMerge}
import models.pdv.{PDVNotFoundResponse, PDVResponse, PDVResponseData, PDVSuccessResponse}
import org.mongodb.scala.MongoException
import play.api.Logging
import repositories.{IndividualDetailsRepoTrait, SessionRepository}
import uk.gov.hmrc.http.HeaderCarrier
import util.FMNConstants.EmptyString

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[IndividualDetailsServiceImpl])
trait IndividualDetailsService {

  def cacheOrigin(userAnswers: UserAnswers, origin: Option[String]): Future[UserAnswers]

  def getNPSPostCode(idData: IndividualDetails): String

  def getIdData(pdvData: PDVResponseData)(
    implicit hc: HeaderCarrier): Future[Either[IndividualDetailsError, IndividualDetails]]

  def getIndividualDetailsAddress(nino: IndividualDetailsNino)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[IndividualDetailsError, Address]]

  def getIndividualDetails(nino: IndividualDetailsNino)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): IndividualDetailsResponseEnvelope[IndividualDetails]

  def createIndividualDetailsData(sessionId: String, individualDetails: IndividualDetails): Future[String]

  def getIndividualDetailsData(nino: String): Future[Option[IndividualDetailsDataCache]]

}

class IndividualDetailsServiceImpl @Inject()(
                                              individualDetailsConnector: IndividualDetailsConnector,
                                              individualDetailsRepository: IndividualDetailsRepoTrait,
                                              sessionRepository: SessionRepository
                                            )(implicit ec: ExecutionContext)
  extends IndividualDetailsService with Logging {

  override def cacheOrigin(userAnswers: UserAnswers, origin: Option[String]): Future[UserAnswers] = {
    for {
      updatedAnswers <- Future.fromTry(userAnswers.set(OriginCacheable, origin.getOrElse("None")))
      _ <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers
  }

  override def getNPSPostCode(idData: IndividualDetails): String =
    getAddressTypeResidential(idData.addressList).addressPostcode.map(_.value).getOrElse("")

  override def getIdData(pdvData: PDVResponseData)(implicit hc: HeaderCarrier): Future[Either[IndividualDetailsError, IndividualDetails]] = {
    getIndividualDetails(IndividualDetailsNino(pdvData.personalDetails match {
      case Some(data) => data.nino.nino
      case None =>
        logger.warn("No Personal Details found in PDV data.")
        EmptyString
    })).value
  }

  override def getIndividualDetailsAddress(nino: IndividualDetailsNino)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[IndividualDetailsError, Address]] = {
    val idAddress = for {
      idData <- getIndividualDetails(nino)
      idDataAddress = idData.addressList.getAddress.filter(_.addressType.equals(ResidentialAddress)).head
    } yield idDataAddress

    idAddress.value.flatMap {
      case Right(address) => Future.successful(Right(address))
      case Left(error) => Future.successful(Left(error))
    }.recover {
      case ex =>
        logger.warn(s"Error while fetching Individual Details Address for $nino", ex)
        Left(InvalidIdentifier(nino))
    }
  }

  override def getIndividualDetails(nino: IndividualDetailsNino
                          )(implicit ec: ExecutionContext, hc: HeaderCarrier): IndividualDetailsResponseEnvelope[IndividualDetails] = {
    implicit val correlationId: CorrelationId = CorrelationId(UUID.randomUUID())
    IndividualDetailsResponseEnvelope.fromEitherF(
      individualDetailsConnector.getIndividualDetails(nino, ResolveMerge('Y')).value
    )
  }

  override def createIndividualDetailsData(sessionId: String, individualDetails: IndividualDetails): Future[String] = {
    individualDetailsRepository.insertOrReplaceIndividualDetailsData(
      getIndividualDetailsData(sessionId, individualDetails)
    )
  }

  override def getIndividualDetailsData(nino: String): Future[Option[IndividualDetailsDataCache]] =
    individualDetailsRepository.findIndividualDetailsDataByNino(nino) map {
      case Some(individualDetailsData) => Some(individualDetailsData)
      case _ => None
    } recover {
      case e: MongoException =>
        logger.warn(s"Failed finding Individual Details Data by NINO: $nino, ${e.getMessage}")
        None
    }

  private def getIndividualDetailsData(sessionId: String, individualDetails: IndividualDetails): IndividualDetailsDataCache = {
    val iDetails = IndividualDetailsData(
      individualDetails.getFirstForename,
      individualDetails.getLastName,
      individualDetails.dateOfBirth.toString,
      individualDetails.getPostCode,
      individualDetails.getNinoWithoutSuffix
    )
    IndividualDetailsDataCache(
      sessionId,
      Some(iDetails)
    )
  }

  private def getAddressTypeResidential(addressList: AddressList): Address = {
    val residentialAddress = addressList.getAddress.filter(_.addressType.equals(ResidentialAddress))
    residentialAddress.head
  }

}
