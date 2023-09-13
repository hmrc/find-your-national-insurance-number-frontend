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

import controllers.actions._
import forms.SelectNINOLetterAddressFormProvider
import models.nps.{LetterIssuedResponse, NPSFMNRequest, RLSDLONFAResponse}
import models.{Mode, PersonDetailsResponse, PersonDetailsSuccessResponse, SelectNINOLetterAddress}
import navigation.Navigator
import pages.SelectNINOLetterAddressPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{CitizenDetailsService, NPSFMNService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SelectNINOLetterAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

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
                                       citizenDetailsService: CitizenDetailsService,
                                       npsFMNService: NPSFMNService
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val emptyString: String = ""
  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val preparedForm = request.userAnswers.get(SelectNINOLetterAddressPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

      for {
        personalDetails <- citizenDetailsService.getPersonalDetails(request.nino.getOrElse(emptyString))
        postCode = getPostCode(personalDetails)
      } yield Ok(view(preparedForm, mode, postCode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val nino = request.nino.getOrElse(emptyString)

      form.bindFromRequest().fold(
        formWithErrors =>
          for {
            personalDetails <- citizenDetailsService.getPersonalDetails(nino)
            postCode = getPostCode(personalDetails)
          } yield BadRequest(view(formWithErrors, mode, postCode)),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SelectNINOLetterAddressPage, value))
            _ <- sessionRepository.set(updatedAnswers)
            personalDetails <- citizenDetailsService.getPersonalDetails(nino)
            status <- npsFMNService.updateDetails(nino, getNPSFMNRequest(personalDetails))
          } yield {
            updatedAnswers.get(SelectNINOLetterAddressPage) match {
              case Some(SelectNINOLetterAddress.NotThisAddress) =>
                Redirect(navigator.nextPage(SelectNINOLetterAddressPage, mode, updatedAnswers))
              case Some(SelectNINOLetterAddress.Postcode) =>
                status match {
                  case LetterIssuedResponse => Redirect(navigator.nextPage(SelectNINOLetterAddressPage, mode, updatedAnswers))
                  case RLSDLONFAResponse => Redirect(routes.SendLetterErrorController.onPageLoad(mode))
                  case _ => Redirect(routes.TechnicalErrorController.onPageLoad())
                }
            }
          }
      )
  }

  private def getPostCode(personDetailsResponse: PersonDetailsResponse): String =
    personDetailsResponse match {
      case PersonDetailsSuccessResponse(pd) => pd.address.map(_.postcode.get).getOrElse(emptyString)
      case _                   => emptyString
    }

  private def getNPSFMNRequest(personDetailsResponse: PersonDetailsResponse): NPSFMNRequest =
    personDetailsResponse match {
      case PersonDetailsSuccessResponse(pd) =>
        NPSFMNRequest(
          pd.person.firstName.getOrElse(emptyString),
          pd.person.lastName.getOrElse(emptyString),
          pd.person.dateOfBirth.map(_.toString).getOrElse(emptyString),
          pd.address.map(_.postcode.get).getOrElse(emptyString)
        )
      case _                   => NPSFMNRequest(emptyString, emptyString, emptyString, emptyString)
    }

}
