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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "find-your-national-insurance-number"

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(host + request.uri).encodedUrl}"

  val loginUrl: String         = configuration.get[String]("urls.login")
  val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String       = configuration.get[String]("urls.signOut")
  val registerUrl: String      = configuration.get[String]("urls.register")
  val storeMyNinoUrl: String   = configuration.get[String]("urls.storeMyNinoUrl")

  private val exitSurveyBaseUrl: String = configuration.get[Service]("microservice.services.feedback-frontend").baseUrl
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/find-your-national-insurance-number"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Int = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  lazy val citizenDetailsServiceUrl: String = configuration.get[Service]("microservice.services.citizen-details").baseUrl

  lazy val pdvBaseUrl: String = configuration.get[Service]("microservice.services.personal-details-validation").baseUrl

  val getNinoUrl: String = configuration.get[String]("external-url.get-nino.CA5403form")

  val printAndPostServiceUrl: String = configuration.get[String]("external-url.national-insurance-number-letter-frontend.host")

  val fmnGuidancePageUrl: String = configuration.get[String]("urls.fmnGuidancePage")

  val hmrcExtraSupportUrl: String = configuration.get[String]("urls.hmrcExtraSupport")
  val callChargesUrl: String = configuration.get[String]("urls.callCharges")

  val ninoByPostServiceUrl: String = configuration.get[Service]("microservice.services.national-insurance-number-by-post").baseUrl

  def individualDetails: DesApiServiceConfig =
    DesApiServiceConfig(configuration.get[Configuration]("microservice.services.individual-details"))

  val individualDetailsServiceUrl: String = configuration.get[String]("external-url.individual-details.host")

  def cacheSecretKey:                 String      = configuration.get[String]("cache.secret-key")


}
