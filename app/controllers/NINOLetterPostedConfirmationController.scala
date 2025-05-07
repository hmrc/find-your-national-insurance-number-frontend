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
import org.apache.commons.lang3.StringUtils
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NINOLetterPostedConfirmationView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class NINOLetterPostedConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  val controllerComponents: MessagesControllerComponents,
  view: NINOLetterPostedConfirmationView,
  sessionCacheService: SessionCacheService
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = identify { implicit request =>
    val nino = request.session.data.getOrElse("nino", StringUtils.EMPTY)
    sessionCacheService.invalidateCache(nino, request.userId)
    val lang = request.lang(messagesApi)
    if (lang.language.equals("cy")) {
      Ok(view(getWelshDate(lang)))
    } else {
      Ok(view(LocalDate.now.format(DateTimeFormatter.ofPattern("d MMMM uuuu"))))
    }
  }

  private def getWelshDate(lang: Lang): String = {
    val month                          = LocalDate.now().getMonth.toString.toLowerCase.capitalize
    val monthKeys: Map[String, String] = Map(
      "January"   -> "month.january",
      "February"  -> "month.february",
      "March"     -> "month.march",
      "April"     -> "month.april",
      "May"       -> "month.may",
      "June"      -> "month.june",
      "July"      -> "month.july",
      "August"    -> "month.august",
      "September" -> "month.september",
      "October"   -> "month.october",
      "November"  -> "month.november",
      "December"  -> "month.december"
    )

    val welshMonth = monthKeys.get(month) match {
      case Some(monthKey) => messagesApi(monthKey)(lang)
      case None           => throw new IllegalArgumentException("Invalid month name")
    }

    LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM uuuu")).replace(month, welshMonth)
  }
}
