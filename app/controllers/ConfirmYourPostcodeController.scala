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
import forms.ConfirmYourPostcodeFormProvider
import models.errors.IndividualDetailsError
import models.individualdetails.AddressType.ResidentialAddress
import models.individualdetails.{Address, ResolveMerge}
import models.nps.{LetterIssuedResponse, RLSDLONFAResponse, TechnicalIssueResponse}
import models.pdv.{PDVResponseData, PersonalDetails}

import javax.inject.Inject
import models.{CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope, Mode, NormalMode}
import pages.ConfirmYourPostcodePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{AuditService, IndividualDetailsService, NPSFMNService, PersonalDetailsValidationService}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.FMNConstants.EmptyString
import views.html.ConfirmYourPostcodeView
import util.FMNHelper
import util.FMNHelper.comparePostCode

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ConfirmYourPostcodeController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireValidData: ValidCustomerDataRequiredAction,
                                        formProvider: ConfirmYourPostcodeFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ConfirmYourPostcodeView,
                                        personalDetailsValidationService: PersonalDetailsValidationService,
                                        individualDetailsService: IndividualDetailsService,
                                        npsFMNService: NPSFMNService,
                                        auditService: AuditService,
                                        individualDetailsConnector: IndividualDetailsConnector,
                                        appConfig: FrontendAppConfig
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireValidData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(ConfirmYourPostcodePage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireValidData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        userEnteredPostCode => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ConfirmYourPostcodePage, userEnteredPostCode))
            _ <- sessionRepository.set(updatedAnswers)
            nino = request.session.data.getOrElse("nino", EmptyString)
            pdvData <- personalDetailsValidationService.getPersonalDetailsValidationByNino(nino)
            idAddress <- getIndividualDetailsAddress(IndividualDetailsNino(nino))
            redirectBasedOnMatch <- pdvData match {
              case Some(pdvValidData) =>
                checkUserEnteredPostcodeMatchWithNPSPostCode(mode, userEnteredPostCode, pdvData, idAddress, pdvValidData)
              case None => Future(Redirect(routes.TechnicalErrorController.onPageLoad()))
            }
          } yield redirectBasedOnMatch
        }
      )
  }

  private def checkUserEnteredPostcodeMatchWithNPSPostCode(mode: Mode,
                                                           userEnteredPostCode: String,
                                                           pdvData: Option[PDVResponseData],
                                                           idAddress: Either[IndividualDetailsError, Address],
                                                           pdvValidData: PDVResponseData)(implicit hc: HeaderCarrier): Future[Result] =
    pdvValidData.npsPostCode match {
      case Some(npsPostCode) if comparePostCode(npsPostCode, userEnteredPostCode) =>
        idAddress match {
          case Right(idAddr) =>
            auditService.findYourNinoConfirmPostcode(userEnteredPostCode, Some(idAddr), pdvData, Some("true"))
        }
        npsLetterChecks(pdvValidData, mode)
      case None =>
        auditService.findYourNinoConfirmPostcode(userEnteredPostCode, None, pdvData, Some("false"))
        Future(Redirect(routes.TechnicalErrorController.onPageLoad()))
      case _ =>
        auditService.findYourNinoConfirmPostcode(userEnteredPostCode, None, pdvData, Some("false"))
        Future(Redirect(routes.EnteredPostCodeNotFoundController.onPageLoad(mode = NormalMode)))
    }

  private def npsLetterChecks(personalDetailsResponse: PDVResponseData, mode: Mode)(implicit hc: HeaderCarrier): Future[Result] = {
    personalDetailsResponse.personalDetails match {
      case Some(personalDetails: PersonalDetails) =>
        for {
          idData <- individualDetailsService.getIndividualDetailsData(personalDetails.nino.nino)
          status <- npsFMNService.sendLetter(personalDetails.nino.nino, FMNHelper.createNPSFMNRequest(idData))
        } yield status match {
          case LetterIssuedResponse() =>
            Redirect(routes.NINOLetterPostedConfirmationController.onPageLoad())
          case RLSDLONFAResponse(responseStatus, responseMessage) =>
            auditService.findYourNinoTechnicalError(personalDetailsResponse, personalDetails, responseStatus, responseMessage)
            Redirect(routes.SendLetterErrorController.onPageLoad(mode))
          case TechnicalIssueResponse(responseStatus, responseMessage) =>
            auditService.findYourNinoTechnicalError(personalDetailsResponse, personalDetails, responseStatus, responseMessage)
            Redirect(routes.TechnicalErrorController.onPageLoad())
          case _ =>
            logger.warn("Unknown NPS FMN API response")
            Redirect(routes.TechnicalErrorController.onPageLoad())
        }
      case None => Future(Redirect(routes.TechnicalErrorController.onPageLoad()))
    }
  }

  def getIndividualDetailsAddress(nino: IndividualDetailsNino
                                 )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Either[IndividualDetailsError, Address]] = {
    implicit val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCrypto(appConfig.cacheSecretKey)
    implicit val correlationId: CorrelationId = CorrelationId(UUID.randomUUID())
    val idAddress = for {
      idData <- IndividualDetailsResponseEnvelope.fromEitherF(individualDetailsConnector.getIndividualDetails(nino, ResolveMerge('Y')).value)
      idDataAddress = idData.addressList.getAddress.filter(_.addressType.equals(ResidentialAddress)).head
    } yield idDataAddress
    idAddress.value
  }

}
