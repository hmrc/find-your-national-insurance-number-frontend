package controllers

import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.Mode
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CheckDetailsController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        val controllerComponents: MessagesControllerComponents
                                      )(implicit ec: ExecutionContext, appConfig: FrontendAppConfig) extends FrontendBaseController with I18nSupport {

  def onPageLoad(validationId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      // TODO Step 1:- PDV Validation logic
      // TODO Step 2:- API 1694 integration

      val postCodeMatched = true // TODO expecting flag value after performing these two steps
      if(postCodeMatched)
        Redirect(routes.ValidDataNINOHelpController.onPageLoad())
      else
        Redirect(routes.InvalidDataNINOHelpController.onPageLoad())
  }

}