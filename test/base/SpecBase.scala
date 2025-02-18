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

package base

import config.FrontendAppConfig
import controllers.actions._
import models.UserAnswers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import util.WireMockSupport

import scala.reflect.ClassTag

class SpecBase extends WireMockSupport with MockitoSugar with GuiceOneAppPerSuite {

  implicit lazy val application: Application           = applicationBuilder().build()
  implicit lazy val applicationWithConfig: Application = applicationBuilderWithConfig().build()
  implicit val hc: HeaderCarrier                       = HeaderCarrier()
  val userAnswersId: String                            = "id"

  implicit val config: FrontendAppConfig = mock[FrontendAppConfig]

  def emptyUserAnswers: UserAnswers = UserAnswers()

  def injector: Injector                               = app.injector
  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  def messagesApi: MessagesApi    = injector.instanceOf[MessagesApi]
  implicit def messages: Messages = messagesApi.preferred(fakeRequest)

  protected val nonEmptyUserAnswers: UserAnswers = UserAnswers(Json.obj("test" -> "test"))

  protected def applicationBuilder(userAnswers: UserAnswers = UserAnswers()): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[ValidCustomerDataRequiredAction].to[ValidCustomerDataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  protected def applicationBuilderWithConfig(
    config: Map[String, Any] = Map(),
    userAnswers: UserAnswers = UserAnswers()
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        config ++ Map(
          "microservice.services.auth.port" -> wiremockPort,
          "microservice.host"               -> "http://localhost:9900/fmn"
        )
      )
      .overrides(
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  protected def applicationBuilderCl50OnWithConfig(
    config: Map[String, Any] = Map(),
    userAnswers: UserAnswers = UserAnswers()
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        config ++ Map(
          "microservice.services.auth.port" -> wiremockPort,
          "microservice.host"               -> "http://localhost:9900/fmn"
        )
      )
      .overrides(
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  def injected[T](c: Class[T]): T = app.injector.instanceOf(c)

  def injected[T](implicit evidence: ClassTag[T]): T = app.injector.instanceOf[T]

  implicit lazy val cc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  implicit class StringOps(s: String) {
    def removeAllNonces(): String = s.replaceAll("""nonce="[^"]*"""", "")
  }

}
