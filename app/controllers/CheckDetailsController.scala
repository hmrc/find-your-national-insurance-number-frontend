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

package controllers

import config.FrontendAppConfig
import connectors.IndividualDetailsConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.IndividualDetailsResponseEnvelope.IndividualDetailsResponseEnvelope
import models.individualdetails.AccountStatusType.FullLive
import models.individualdetails.AddressStatus.NotDlo
import models.individualdetails.AddressType.ResidentialAddress
import models.individualdetails.CrnIndicator.False
import models.individualdetails.{AddressList, IndividualDetails, ResolveMerge}
import models.{CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope, Mode, PersonalDetailsValidation}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PersonalDetailsValidationService
import uk.gov.hmrc.crypto.SymmetricCryptoFactory
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CheckDetailsController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        personalDetailsValidationService: PersonalDetailsValidationService,
                                        individualDetailsConnector: IndividualDetailsConnector,
                                        val controllerComponents: MessagesControllerComponents
                                      )(implicit ec: ExecutionContext, appConfig: FrontendAppConfig) extends FrontendBaseController with I18nSupport with Logging {


  def onPageLoad(mode: Mode, validationId: String): Action[AnyContent] = (identify andThen getData andThen requireData) async {
    implicit request => {


      val validationId = "68c0fcdf-05fc-474a-baee-4f653e5b026b"


      for {
        pdvDataId <- personalDetailsValidationService.createPDVFromValidationId(validationId)
        pdvData <- personalDetailsValidationService.getPersonalDetailsValidationByValidationId(pdvDataId)
        idData <- getIndividualDetails(IndividualDetailsNino(pdvData.get.personalDetails.get.nino.nino)).value
      } yield idData.fold(
        error => Ok(error.errorMessage),
        individualDetailsData => {
          val pdvPostCode: String = pdvData.get.personalDetails.get.postCode.get
          val check = checkConditions(individualDetailsData, pdvPostCode)

          logData(individualDetailsData, pdvData.get)

          if (check == true) {
            Redirect(routes.ValidDataNINOHelpController.onPageLoad())
          } else {
            Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode = mode))
          }
        })
    }
  }

  def logData(r: IndividualDetails, p: PersonalDetailsValidation): Unit = {
    logger.info("*****************" + r.toString)
    logger.info("*****************" + p.toString)
  }


  def checkConditions(idData: IndividualDetails, pdvPostCode: String): Boolean = {
    idData.accountStatusType.get.equals(FullLive) &&
    idData.crnIndicator.equals(False) &&
    getAddressTypeResidential(idData.addressList).addressStatus.get.equals(NotDlo) &&
    getAddressTypeResidential(idData.addressList).addressPostcode.get.value.equals(pdvPostCode)
  }


  def getAddressTypeResidential(addressList: AddressList) = {
    val residentialAddress = addressList.address.get.filter(_.addressType.equals(ResidentialAddress))
    residentialAddress(0)
  }


  def getIndividualDetails(nino: IndividualDetailsNino
                          )(implicit ec: ExecutionContext, hc: HeaderCarrier): IndividualDetailsResponseEnvelope[IndividualDetails] = {
    implicit val crypto = SymmetricCryptoFactory.aesCrypto(appConfig.cacheSecretKey)
    implicit val correlationId = CorrelationId(UUID.randomUUID())
    for {
      individualDetails <- individualDetailsConnector.getIndividualDetails(nino, ResolveMerge('Y'))
      dd <- IndividualDetailsResponseEnvelope(Option(individualDetails).get)
    } yield dd

  }
}
