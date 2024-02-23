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

package controllers

import config.FrontendAppConfig
import connectors.IndividualDetailsConnector
import controllers.actions._
import forms.SelectNINOLetterAddressFormProvider
import models.errors._
import models.individualdetails.AddressType.ResidentialAddress
import models.individualdetails.{Address, ResolveMerge}
import models.nps.{LetterIssuedResponse, NPSFMNRequest, RLSDLONFAResponse, TechnicalIssueResponse}
import models.pdv.PDVResponseData
import models.{CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope, Mode, SelectNINOLetterAddress, UserAnswers}
import navigation.Navigator
import pages.SelectNINOLetterAddressPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{AuditService, NPSFMNService, PersonalDetailsValidationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SelectNINOLetterAddressView
import org.apache.commons.lang3.StringUtils
import play.api.Logging
import play.api.data.Form
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class SelectNINOLetterAddressController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   sessionRepository: SessionRepository,
                                                   navigator: Navigator,
                                                   identify: IdentifierAction,
                                                   getData: DataRetrievalAction,
                                                   requireData: DataRequiredAction,
                                                   formProvider: SelectNINOLetterAddressFormProvider,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: SelectNINOLetterAddressView,
                                                   personalDetailsValidationService: PersonalDetailsValidationService,
                                                   auditService: AuditService,
                                                   npsFMNService: NPSFMNService,
                                                   individualDetailsConnector: IndividualDetailsConnector,
                                                   appConfig: FrontendAppConfig
                                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[SelectNINOLetterAddress] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(SelectNINOLetterAddressPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      for {
        pdvData <- personalDetailsValidationService.getPersonalDetailsValidationByNino(request.session.data.getOrElse("nino", ""))
        postCode = getPostCode(pdvData)
      } yield Ok(view(preparedForm, mode, postCode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val nino = request.session.data.getOrElse("nino", StringUtils.EMPTY)

      personalDetailsValidationService.getPersonalDetailsValidationByNino(nino).flatMap(pdvData =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, mode, getPostCode(pdvData)))),
          value => {
            request.userAnswers.set(SelectNINOLetterAddressPage, value) match {
              case Failure(_) => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
              case Success(uA) =>
                sessionRepository.set(uA)
                uA.get(SelectNINOLetterAddressPage) match {
                  case Some(SelectNINOLetterAddress.NotThisAddress) =>
                    auditAddress(pdvData, nino, value.toString)
                    Future.successful(Redirect(navigator.nextPage(SelectNINOLetterAddressPage, mode, uA)))
                  case Some(SelectNINOLetterAddress.Postcode) =>
                    sendLetter(nino, pdvData, value.toString, uA, mode)
                }
            }
          }
        )
      )
  }

  private def sendLetter(nino: String, pdvData: Option[PDVResponseData], value: String, uA: UserAnswers, mode: Mode)
                        (implicit headerCarrier: HeaderCarrier): Future[Result] = {
    npsFMNService.sendLetter(nino, getNPSFMNRequest(pdvData)) map {
      case LetterIssuedResponse() =>
        auditAddress(pdvData, nino, value)
        Redirect(navigator.nextPage(SelectNINOLetterAddressPage, mode, uA))
      case RLSDLONFAResponse(responseStatus, responseMessage) =>
        auditService.findYourNinoError(pdvData, Some(responseStatus.toString), responseMessage)
        Redirect(routes.SendLetterErrorController.onPageLoad(mode))
      case TechnicalIssueResponse(responseStatus, responseMessage) =>
        auditService.findYourNinoError(pdvData, Some(responseStatus.toString), responseMessage)
        Redirect(routes.TechnicalErrorController.onPageLoad())
      case _ =>
        logger.warn("Unknown NPS FMN API response")
        Redirect(routes.TechnicalErrorController.onPageLoad())
    }
  }

  private def getPostCode(pdvResponseData: Option[PDVResponseData]): String =
    pdvResponseData match {
      case Some(pd) => pd.getPostCode
      case _ => StringUtils.EMPTY
    }

  private def getNPSFMNRequest(pdvData: Option[PDVResponseData]): NPSFMNRequest =
    pdvData match {
      case Some(pd) if pd.personalDetails.isDefined =>
        NPSFMNRequest(
          pd.getFirstName,
          pd.getLastName,
          pd.getDateOfBirth,
          pd.getPostCode
        )
      case _ => NPSFMNRequest.empty
    }

  def getIndividualDetailsAddress(nino: IndividualDetailsNino)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[IndividualDetailsError, Address]] = {
    implicit val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCrypto(appConfig.cacheSecretKey)
    implicit val correlationId: CorrelationId = CorrelationId(UUID.randomUUID())
    val idAddress = for {
      idData <- IndividualDetailsResponseEnvelope.fromEitherF(individualDetailsConnector.getIndividualDetails(nino, ResolveMerge('Y')).value)
      idDataAddress = idData.addressList.getAddress.filter(_.addressType.equals(ResidentialAddress)).head
    } yield idDataAddress
    idAddress.value
  }

  private def auditAddress(pdvData: Option[PDVResponseData], nino: String, value: String)(
    implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit] = {
    getIndividualDetailsAddress(IndividualDetailsNino(nino)) map {
      case Right(idAddress) => auditService.findYourNinoOnlineLetterOption(pdvData, idAddress, value)
      case Left(individualDetailsError) =>
        val statusCode = individualDetailsError match {
          case conError: ConnectorError => Some(conError.statusCode.toString)
          case _ => None
        }
        val responseMessage = "Could not get individuals address"
        auditService.findYourNinoError(pdvData, statusCode, responseMessage)
        throw new IllegalArgumentException(responseMessage)
    }
  }
}