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
import controllers.bindable.Origin
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import java.net.URLEncoder

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "find-your-national-insurance-number"

  val gtmContainer: String = configuration.get[String]("tracking-consent-frontend.gtm.container")
  lazy val trackingHost: String = getExternalUrl(s"tracking-frontend.host").getOrElse("")
  lazy val trackingServiceUrl = s"$trackingHost/track"
  val enc = URLEncoder.encode(_: String, "UTF-8")

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(host + request.uri).encodedUrl}"

  val loginUrl: String         = configuration.get[String]("urls.login")
  val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String       = configuration.get[String]("urls.signOut")
  val registerUrl: String      = configuration.get[String]("urls.register")
  val storeMyNinoUrl: String   = configuration.get[String]("urls.storeMyNinoUrl")

  private val exitSurveyBaseUrl: String = configuration.get[Service]("microservice.services.feedback-frontend").baseUrl
  val exitSurveyUrl: String             = s"$exitSurveyBaseUrl/feedback/find-your-national-insurance-number"

  lazy val feedbackSurveyFrontendHost = getExternalUrl(s"feedback-survey-frontend.host").getOrElse("")
  lazy val basGatewayFrontendHost = getExternalUrl(s"bas-gateway-frontend.host").getOrElse("")

  val defaultOrigin: Origin = Origin("FIND_MY_NINO")
  private def getExternalUrl(key: String): Option[String] =
    configuration.getOptional[String](s"external-url.$key")
  def getFeedbackSurveyUrl(origin: Origin): String =
    feedbackSurveyFrontendHost + "/feedback/" + enc(origin.origin) + "/nino"

  def getBasGatewayFrontendSignOutUrl(continueUrl: String): String =
    basGatewayFrontendHost + s"/bas-gateway/sign-out-without-state?continue=$continueUrl"

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

  lazy val SCAWrapperEnabled = configuration.getOptional[Boolean]("features.sca-wrapper-enabled").getOrElse(false)
  
  val ninoByPostServiceUrl: String = configuration.get[Service]("microservice.services.national-insurance-number-by-post").baseUrl

  lazy val accessibilityStatementToggle: Boolean =
    configuration.getOptional[Boolean](s"accessibility-statement.toggle").getOrElse(false)
  lazy val accessibilityBaseUrl: String = servicesConfig.getString("accessibility-statement.baseUrl")
  lazy private val accessibilityRedirectUrl =
    servicesConfig.getString("accessibility-statement.redirectUrl")

  def accessibilityStatementUrl(referrer: String) =
    s"$accessibilityBaseUrl/accessibility-statement$accessibilityRedirectUrl?referrerUrl=${SafeRedirectUrl(accessibilityBaseUrl + referrer).encodedUrl}"


  def individualDetails: DesApiServiceConfig =
    DesApiServiceConfig(configuration.get[Configuration]("microservice.services.individual-details"))

  lazy val individualDetailsProtocol: String = configuration.get[String]("external-url.individual-details.protocol")
  lazy val individualDetailsHost: String = configuration.get[String]("external-url.individual-details.host")
  lazy val individualDetailsBaseUrl: String = configuration.get[String]("external-url.individual-details.base-url")
  lazy val individualDetailsPort: String = configuration.get[String]("external-url.individual-details.port")

  val individualDetailsServiceUrl: String = s"$individualDetailsProtocol://$individualDetailsHost:$individualDetailsPort$individualDetailsBaseUrl"

  lazy val npsFMNAPIProtocol: String = configuration.get[String]("external-url.nps-fmn-api.protocol")
  lazy val npsFMNAPIHost: String = configuration.get[String]("external-url.nps-fmn-api.host")
  lazy val npsFMNAPIBaseUrl: String = configuration.get[String]("external-url.nps-fmn-api.base-url")
  lazy val npsFMNAPIPort: String = configuration.get[String]("external-url.nps-fmn-api.port")

  val npsFMNAPIUrl: String = s"$npsFMNAPIProtocol://$npsFMNAPIHost:$npsFMNAPIPort$npsFMNAPIBaseUrl"

  def cacheSecretKey:String = configuration.get[String]("cache.secret-key")

  // banners
  val showAlphaBanner: Boolean = configuration.get[Boolean]("sca-wrapper.banners.show-alpha")
  val showBetaBanner: Boolean = configuration.get[Boolean]("sca-wrapper.banners.show-beta")
  val showHelpImproveBanner: Boolean = configuration.get[Boolean]("sca-wrapper.banners.show-help-improve")
  val showChildBenefitBanner: Boolean = configuration.get[Boolean]("sca-wrapper.banners.show-child-benefit")

  val npsFMNAppStatusMessageList:String = configuration.get[String]("npsfmn.app-status-message-list")

}
