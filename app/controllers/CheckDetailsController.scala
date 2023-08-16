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
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PersonalDetailsValidationService
import connectors.IndividualDetailsConnector

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}

import models.IndividualDetailsResponseEnvelope.IndividualDetailsResponseEnvelope
import models.{IndividualDetailsResponseEnvelope, CorrelationId, IndividualDetailsNino}
import models.individualdetails.{IndividualDetails, ResolveMerge}
import models.Mode

import uk.gov.hmrc.crypto.SymmetricCryptoFactory
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.util.UUID
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

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


      // TODO Step 1:- PDV Validation logic
      personalDetailsValidationService.createPDVFromValidationId(validationId).onComplete {
        case Success(value) => {
          logger.info(value)
          personalDetailsValidationService.getPersonalDetailsValidationByValidationId(validationId).onComplete {
            case Success(pdv) => {
              logger.info("pdv result: " + pdv)
              val ninoFromPDV = pdv.map(_.personalDetails.map(_.nino).getOrElse(""))
              val postCodeFromPDV = pdv.map(_.personalDetails.map(_.postCode).getOrElse(""))
            }
            case Failure(ex) => logger.warn(ex.getMessage)
          }
        }
        case Failure(ex) => logger.warn(ex.getMessage)
      }


      // TODO Step 2:- API 1694 integration
      val nino = "AB049513"
      for {
        id <- getIndividualDetails(IndividualDetailsNino(nino)).value
      } yield id.fold(
        error => Ok(error.errorMessage),
        success => {
          Ok(success.ninoWithoutSuffix + " " +
            success.ninoSuffix.get.value + "\n" +
            success.accountStatusType.get + "\n" +
            success.addressList.address.get(0).addressStatus.get)
        })

//      val postCodeMatched = true // TODO expecting flag value after performing these two steps
//      if(postCodeMatched) {
//        Redirect(routes.ValidDataNINOHelpController.onPageLoad())
//      } else {
//        Redirect(routes.InvalidDataNINOHelpController.onPageLoad(mode))
//      }

    }
  }


  def getIndividualDetails(nino: IndividualDetailsNino
                          )(implicit ec: ExecutionContext, hc: HeaderCarrier): IndividualDetailsResponseEnvelope[IndividualDetails]     = {
    implicit val crypto = SymmetricCryptoFactory.aesCrypto(appConfig.cacheSecretKey)
    implicit val correlationId = CorrelationId(UUID.randomUUID())
    for {
      individualDetails <- individualDetailsConnector.getIndividualDetails(nino, ResolveMerge('Y'))
      dd <- IndividualDetailsResponseEnvelope(Option(individualDetails).get)
    } yield dd

    }
}
