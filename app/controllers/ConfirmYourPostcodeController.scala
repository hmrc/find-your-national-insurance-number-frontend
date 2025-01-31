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

import controllers.actions._
import forms.ConfirmYourPostcodeFormProvider
import models.errors.IndividualDetailsError
import models.individualdetails.Address
import models.pdv.PDVResponseData
import models.{IndividualDetailsNino, Mode, NormalMode, OriginType}
import org.apache.commons.lang3.StringUtils
import pages.ConfirmYourPostcodePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{AuditService, IndividualDetailsService, PersonalDetailsValidationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import util.FMNHelper.comparePostCode
import views.html.ConfirmYourPostcodeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmYourPostcodeController @Inject() (
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
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireValidData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(ConfirmYourPostcodePage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }
      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireValidData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          userEnteredPostCode =>
            for {
              updatedAnswers       <- Future.fromTry(request.userAnswers.set(ConfirmYourPostcodePage, userEnteredPostCode))
              _                    <- sessionRepository.set(updatedAnswers)
              nino                  = request.session.data.getOrElse("nino", StringUtils.EMPTY)
              pdvData              <- personalDetailsValidationService.getPersonalDetailsValidationByNino(nino)
              idAddress            <- individualDetailsService.getIndividualDetailsAddress(IndividualDetailsNino(nino))
              redirectBasedOnMatch <- pdvData match {
                                        case Some(pdvValidData) =>
                                          checkUserEnteredPostcodeMatchWithNPSPostCode(
                                            userEnteredPostCode,
                                            idAddress,
                                            pdvValidData,
                                            request.origin
                                          )
                                        case None               => throw new IllegalArgumentException("No pdv data found")
                                      }
            } yield redirectBasedOnMatch
        )
  }

  private def checkUserEnteredPostcodeMatchWithNPSPostCode(
    userEnteredPostCode: String,
    idAddress: Either[IndividualDetailsError, Address],
    pdvValidData: PDVResponseData,
    origin: Option[OriginType]
  )(implicit hc: HeaderCarrier): Future[Result] =
    pdvValidData.npsPostCode match {
      case Some(npsPostCode) if comparePostCode(npsPostCode, userEnteredPostCode) =>
        idAddress match {
          case Right(idAddr) =>
            auditService.findYourNinoConfirmPostcode(
              userEnteredPostCode,
              Some(idAddr),
              Some(pdvValidData),
              Some("true"),
              origin
            )
          case _             =>
            throw new IllegalArgumentException("Failed to parse the address from Individual Details")
        }
        Future(Redirect(routes.SelectNINOLetterAddressController.onPageLoad()))
      case None                                                                   =>
        throw new IllegalArgumentException("nps postcode missing")
      case _                                                                      =>
        auditService.findYourNinoConfirmPostcode(userEnteredPostCode, None, Some(pdvValidData), Some("false"), origin)
        Future(Redirect(routes.EnteredPostCodeNotFoundController.onPageLoad(mode = NormalMode)))
    }
}
