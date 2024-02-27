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

import com.google.inject.ImplementedBy
import connectors.IndividualDetailsConnector
import models.IndividualDetailsResponseEnvelope.IndividualDetailsResponseEnvelope
import models.errors.IndividualDetailsError
import models.individualdetails.AccountStatusType.FullLive
import models.individualdetails.AddressStatus.NotDlo
import models.individualdetails.AddressType.ResidentialAddress
import models.individualdetails.CrnIndicator.{False, True}
import models.individualdetails.{Address, AddressList, IndividualDetails, ResolveMerge}
import models.pdv.{PDVRequest, PDVResponseData}
import models.{CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import util.FMNConstants.EmptyString

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[CheckDetailsServiceImpl])
trait CheckDetailsService {
  def checkConditions(idData: IndividualDetails): (Boolean, String)

  def getNPSPostCode(idData: IndividualDetails): String

  def getPDVData(body: PDVRequest)(implicit hc: HeaderCarrier): Future[PDVResponseData]

  def getIdData(pdvData: PDVResponseData)(implicit hc: HeaderCarrier): Future[Either[IndividualDetailsError, IndividualDetails]]

  def getIndividualDetails(nino: IndividualDetailsNino
                          )(implicit ec: ExecutionContext, hc: HeaderCarrier): IndividualDetailsResponseEnvelope[IndividualDetails]
}

class CheckDetailsServiceImpl @Inject()(
                                         personalDetailsValidationService: PersonalDetailsValidationService,
                                         individualDetailsConnector: IndividualDetailsConnector
                                       )(implicit ec: ExecutionContext) extends CheckDetailsService with Logging {

   def checkConditions(idData: IndividualDetails): (Boolean, String) = {
    var reason = EmptyString

    if (!idData.accountStatusType.exists(_.equals(FullLive))) {
      reason += "AccountStatusType is not FullLive;"
    }
    if (idData.crnIndicator.equals(True)) {
      reason += "CRN;"
    }
    if (!getAddressTypeResidential(idData.addressList).addressStatus.exists(_.equals(NotDlo))) {
      reason += "ResidentialAddressStatus is Dlo or Nfa;"
    }

    val status = {
      idData.accountStatusType.exists(_.equals(FullLive)) &&
        idData.crnIndicator.equals(False) &&
        getAddressTypeResidential(idData.addressList).addressStatus.exists(_.equals(NotDlo))
    }

    (status, reason)
  }

  def getNPSPostCode(idData: IndividualDetails): String =
    getAddressTypeResidential(idData.addressList).addressPostcode.map(_.value).getOrElse("")

  def getPDVData(body: PDVRequest)(implicit hc: HeaderCarrier): Future[PDVResponseData] = {
    val p = for {
      pdvData <- personalDetailsValidationService.createPDVDataFromPDVMatch(body)
    } yield pdvData match {
      case data: PDVResponseData =>
        data
      case _ =>
        throw new Exception("No PDV data found")
    }
    p.recover {
      case ex: Exception =>
        logger.debug(ex.getMessage)
        throw ex
    }
  }

  def getIdData(pdvData: PDVResponseData)(implicit hc: HeaderCarrier): Future[Either[IndividualDetailsError, IndividualDetails]] = {
    getIndividualDetails(IndividualDetailsNino(pdvData.personalDetails match {
      case Some(data) => data.nino.nino
      case None =>
        logger.warn("No Personal Details found in PDV data, likely validation failed")
        EmptyString
    })).value
  }

  def getIndividualDetails(nino: IndividualDetailsNino
                                  )(implicit ec: ExecutionContext, hc: HeaderCarrier): IndividualDetailsResponseEnvelope[IndividualDetails] = {
    implicit val correlationId: CorrelationId = CorrelationId(UUID.randomUUID())
    IndividualDetailsResponseEnvelope.fromEitherF(individualDetailsConnector.getIndividualDetails(nino, ResolveMerge('Y')).value)
  }

  private def getAddressTypeResidential(addressList: AddressList): Address = {
    val residentialAddress = addressList.getAddress.filter(_.addressType.equals(ResidentialAddress))
    residentialAddress.head
  }

}
