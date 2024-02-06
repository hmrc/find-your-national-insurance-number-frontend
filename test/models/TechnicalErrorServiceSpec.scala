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

package models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import models.TechnicalErrorService

class TechnicalErrorServiceSpec extends AnyFlatSpec with Matchers {

  "TechnicalErrorService options" should "correctly create a sequence of RadioItems" in {
    implicit val lang: Lang = Lang("en")
    implicit val messagesApi: MessagesApi = stubMessagesApi()
    implicit val messages: Messages = MessagesImpl(lang, messagesApi)

    val result = TechnicalErrorService.options

    result shouldBe Seq(
      RadioItem(
        content = Text(messages("technicalError.tryAgain")),
        value   = Some("tryAgain"),
        id      = Some("value_0"),
        hint    = Some(Hint(content = Text(messages("technicalError.tryAgain.hint"))))
      ),
      RadioItem(
        content = Text(messages("technicalError.printForm")),
        value   = Some("printForm"),
        id      = Some("value_1"),
        hint    = Some(Hint(content = Text(messages("technicalError.printForm.hint"))))
      ),
      RadioItem(
        content = Text(messages("technicalError.phoneHMRC")),
        value   = Some("phoneHMRC"),
        id      = Some("value_2"),
        hint    = Some(Hint(content = Text(messages("technicalError.phoneHMRC.hint"))))
      )
    )
  }
}