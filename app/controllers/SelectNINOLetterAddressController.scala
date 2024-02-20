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
import models.errors.IndividualDetailsError
import models.individualdetails.AddressType.ResidentialAddress
import models.individualdetails.{Address, ResolveMerge}
import models.nps.{LetterIssuedResponse, RLSDLONFAResponse, TechnicalIssueResponse}
import models.pdv.PDVResponseData
import models.{CorrelationId, IndividualDetailsNino, IndividualDetailsResponseEnvelope, Mode, SelectNINOLetterAddress}
import navigation.Navigator
import pages.SelectNINOLetterAddressPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AuditService, IndividualDetailsService, NPSFMNService, PersonalDetailsValidationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SelectNINOLetterAddressView
import org.apache.commons.lang3.StringUtils
import play.api.Logging
import play.api.data.Form
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.http.HeaderCarrier
import util.{AuditUtils, FMNHelper}

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
                                                   individualDetailsService: IndividualDetailsService,
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
      logger.info("SelectNINOLetterAddressController NINO: " + nino)
      form.bindFromRequest().fold(
        formWithErrors =>
          for {
            pdvData <- personalDetailsValidationService.getPersonalDetailsValidationByNino(nino)
            postCode = getPostCode(pdvData)
          } yield BadRequest(view(formWithErrors, mode, postCode)),

        value => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectNINOLetterAddressPage, value))
            _ <- sessionRepository.set(updatedAnswers)
            pdvData <- personalDetailsValidationService.getPersonalDetailsValidationByNino(nino)
            idData <- individualDetailsService.getIndividualDetailsData(nino)
          } yield {
            updatedAnswers.get(SelectNINOLetterAddressPage) match {
              case Some(SelectNINOLetterAddress.NotThisAddress) =>
                for {
                  idAddress <- getIndividualDetailsAddress(IndividualDetailsNino(nino))
                } yield idAddress match {
                  case Right(idAddr) =>
                    auditService.audit(AuditUtils.buildAuditEvent(pdvData.flatMap(_.personalDetails),
                      individualDetailsAddress = Some(idAddr),
                      auditType = "FindYourNinoOnlineLetterOption",
                      validationOutcome = pdvData.map(_.validationStatus).getOrElse(""),
                      identifierType = pdvData.map(_.CRN.getOrElse("")).getOrElse(""),
                      findMyNinoOption = Some(value.toString)
                    ))
                }
                Future.successful(Redirect(navigator.nextPage(SelectNINOLetterAddressPage, mode, updatedAnswers)))
              case Some(SelectNINOLetterAddress.Postcode) =>
                for {
                  idAddress <- getIndividualDetailsAddress(IndividualDetailsNino(nino))
                } yield idAddress match {
                  case Right(idAddr) =>
                    auditService.audit(AuditUtils.buildAuditEvent(pdvData.flatMap(_.personalDetails),
                      individualDetailsAddress = Some(idAddr),
                      auditType = "FindYourNinoOnlineLetterOption",
                      validationOutcome = pdvData.map(_.validationStatus).getOrElse(""),
                      identifierType = pdvData.map(_.CRN.getOrElse("")).getOrElse(""),
                      findMyNinoOption = Some(value.toString)
                    ))
                }

                npsFMNService.sendLetter(nino, FMNHelper.createNPSFMNRequest(idData)).map {
                  case LetterIssuedResponse() =>
                    logger.warn("NPS FMN API response: LetterIssuedResponse")
                    Redirect(navigator.nextPage(SelectNINOLetterAddressPage, mode, updatedAnswers))
                  case RLSDLONFAResponse(responseStatus, responseMessage) =>
                    logger.warn("NPS FMN API response: RLSDLONFAResponse")
                    personalDetailsValidationService.getPersonalDetailsValidationByNino(nino).onComplete {
                      case Success(pdv) =>
                        auditService.audit(AuditUtils.buildAuditEvent(pdv.flatMap(_.personalDetails),
                          auditType = "FindYourNinoError",
                          validationOutcome = pdv.map(_.validationStatus).getOrElse(""),
                          identifierType = pdv.map(_.CRN.getOrElse("")).getOrElse(""),
                          pageErrorGeneratedFrom = Some("/postcode"),
                          errorStatus = Some(responseStatus.toString),
                          errorReason = Some(responseMessage)
                        ))
                      case Failure(ex) => logger.warn(ex.getMessage)
                    }
                    Redirect(routes.SendLetterErrorController.onPageLoad(mode))
                  case TechnicalIssueResponse(responseStatus, responseMessage) =>
                    personalDetailsValidationService.getPersonalDetailsValidationByNino(nino).onComplete {
                      case Success(pdv) =>
                        auditService.audit(AuditUtils.buildAuditEvent(pdv.flatMap(_.personalDetails),
                          auditType = "FindYourNinoError",
                          validationOutcome = pdv.map(_.validationStatus).getOrElse(""),
                          identifierType = pdv.map(_.CRN.getOrElse("")).getOrElse(""),
                          pageErrorGeneratedFrom = Some("/postcode"),
                          errorStatus = Some(responseStatus.toString),
                          errorReason = Some(responseMessage)
                        ))
                      case Failure(ex) => logger.warn(ex.getMessage)
                    }
                    Redirect(routes.TechnicalErrorController.onPageLoad())
                  case _ =>
                    logger.warn("Unknown NPS FMN API response")
                    Redirect(routes.TechnicalErrorController.onPageLoad())
                }
            }
          }
        }.flatten
      )
  }

  private def getPostCode(pdvResponseData: Option[PDVResponseData]): String =
    pdvResponseData match {
      case Some(pd) => pd.getPostCode
      case _ => StringUtils.EMPTY
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
}