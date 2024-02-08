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
import models.individualdetails.AddressType.ResidentialAddress
import models.individualdetails.{Address, ResolveMerge}
import models.nps.{LetterIssuedResponse, NPSFMNRequest, RLSDLONFAResponse, TechnicalIssueResponse}
import models.pdv.PDVResponseData
import models.{CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope, Mode, SelectNINOLetterAddress, UserAnswers}
import navigation.Navigator
import org.apache.commons.lang3.StringUtils
import pages.SelectNINOLetterAddressPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{AuditService, NPSFMNService, PersonalDetailsValidationService}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.AuditUtils
import views.html.SelectNINOLetterAddressView

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
      personalDetailsValidationService.getPersonalDetailsValidationByNino(nino).flatMap(data =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, mode, getPostCode(data)))),
          value => {
            request.userAnswers.set(SelectNINOLetterAddressPage, value) match {
              case Failure(_) => Future.successful(Redirect(routes.TechnicalErrorController.onPageLoad()))
              case Success(uA) =>
                sessionRepository.set(uA)
                uA.get(SelectNINOLetterAddressPage) match {
                  case Some(SelectNINOLetterAddress.NotThisAddress) =>
                    Future.successful(Redirect(navigator.nextPage(SelectNINOLetterAddressPage, mode, uA)))
                  case Some(SelectNINOLetterAddress.Postcode) =>
                    sendLetter(nino, data, value.toString, uA, mode)
                }
            }
          }
        )
      )
  }

  private def sendLetter(nino: String, data: Option[PDVResponseData], value: String, uA: UserAnswers, mode: Mode)
                        (implicit headerCarrier: HeaderCarrier): Future[Result] = {
    npsFMNService.sendLetter(nino, getNPSFMNRequest(data)) map {
      case LetterIssuedResponse() =>
        for {
          idAddress <- getIndividualDetailsAddress(IndividualDetailsNino(nino))
        } yield idAddress match {
          case Right(idAddress) =>
            auditWithAddress(data, idAddress, value)
        }
        Redirect(navigator.nextPage(SelectNINOLetterAddressPage, mode, uA))
      case RLSDLONFAResponse(responseStatus, responseMessage) =>
        auditOnError(data, responseStatus.toString, responseMessage)
        Redirect(routes.SendLetterErrorController.onPageLoad(mode))
      case TechnicalIssueResponse(responseStatus, responseMessage) =>
        auditOnError(data, responseStatus.toString, responseMessage)
        Redirect(routes.TechnicalErrorController.onPageLoad())
      case _ =>
        logger.warn("Unknown NPS FMN API response")
        Redirect(routes.TechnicalErrorController.onPageLoad())
    }
  }

  private def auditWithAddress(data: Option[PDVResponseData], idAddress: Address, value: String)
                              (implicit headerCarrier: HeaderCarrier): Unit = {
    auditService.audit(AuditUtils.buildAuditEvent(data.flatMap(_.personalDetails),
      Some(idAddress),
      "FindYourNinoOnlineLetterOption",
      data.map(_.validationStatus).getOrElse(""),
      data.map(_.CRN.getOrElse("")).getOrElse(""),
      Some(value.toString),
      None,
      None,
      None,
      None,
      None
    ))
  }

  private def auditOnError(data: Option[PDVResponseData], responseStatus: String, responseMessage: String)
                          (implicit headerCarrier: HeaderCarrier): Unit = {
    auditService.audit(AuditUtils.buildAuditEvent(data.flatMap(_.personalDetails),
      None,
      "FindYourNinoError",
      data.map(_.validationStatus).getOrElse(""),
      data.map(_.CRN.getOrElse("")).getOrElse(""),
      None,
      None,
      None,
      Some("/postcode"),
      Some(responseStatus),
      Some(responseMessage)
    ))
  }

  private def getPostCode(pdvResponseData: Option[PDVResponseData]): String =
    pdvResponseData match {
      case Some(pd) => pd.getPostCode
      case _ => StringUtils.EMPTY
    }

  private def getNPSFMNRequest(pdvResponseData: Option[PDVResponseData]): NPSFMNRequest =
    pdvResponseData match {
      case Some(pd) if pd.personalDetails.isDefined =>
        NPSFMNRequest(
          pd.getFirstName,
          pd.getLastName,
          pd.getDateOfBirth,
          pd.getPostCode
        )
      case _ => NPSFMNRequest.empty
    }

  def getIndividualDetailsAddress(nino: IndividualDetailsNino
                                 )(implicit ec: ExecutionContext, hc: HeaderCarrier) = {
    implicit val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCrypto(appConfig.cacheSecretKey)
    implicit val correlationId: CorrelationId = CorrelationId(UUID.randomUUID())
    val idAddress = for {
      idData <- IndividualDetailsResponseEnvelope.fromEitherF(individualDetailsConnector.getIndividualDetails(nino, ResolveMerge('Y')).value)
      idDataAddress = idData.addressList.getAddress.filter(_.addressType.equals(ResidentialAddress)).head
    } yield idDataAddress
    idAddress.value
  }
}
