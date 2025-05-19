/*
 * Copyright 2025 HM Revenue & Customs
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
import models.OriginType
import play.api.Configuration
import play.api.i18n.Lang
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl}

import java.net.URLEncoder

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration) {

  val appName: String       = configuration.get[String]("appName")
  val exitSurveyURL: String = configuration.get[String]("sca-wrapper.signout.url")

  val enc = URLEncoder.encode(_: String, "UTF-8")

  val loginUrl: String                         = configuration.get[String]("urls.login")
  val registerUrl: String                      = configuration.get[String]("urls.register")
  val storeMyNinoUrl: String                   = configuration.get[String]("urls.storeMyNinoUrl")
  val fmnCheckDetailsUrl: String               = configuration.get[String]("urls.fmnCheckDetailsUrl")
  private val accessibilityBaseUrl: String     =
    configuration.get[String]("sca-wrapper.services.accessibility-statement-frontend.url")
  private val accessibilityRedirectUrl: String = configuration.get[String](s"accessibility-statement.service-path")

  def accessibilityStatementUrl(referrer: String): String = {
    val redirectUrl = RedirectUrl(accessibilityBaseUrl + referrer).getEither(
      OnlyRelative | AbsoluteWithHostnameFromAllowlist("localhost")
    ) match {
      case Right(safeRedirectUrl) => safeRedirectUrl.url
      case Left(error)            => throw new IllegalArgumentException(error)
    }
    s"$accessibilityBaseUrl/accessibility-statement$accessibilityRedirectUrl?referrerUrl=$redirectUrl"
  }

  val feedbackSurveyFrontendHost = getExternalUrl(s"feedback-survey-frontend.host").getOrElse("")
  val basGatewayFrontendHost     = getExternalUrl(s"bas-gateway-frontend.host").getOrElse("")

  private def getExternalUrl(key: String): Option[String] =
    configuration.getOptional[String](s"external-url.$key")
  def getFeedbackSurveyUrl(origin: OriginType): String    =
    feedbackSurveyFrontendHost + "/feedback/" + enc(origin.toString)

  def getBasGatewayFrontendSignOutUrl(continueUrl: String): String =
    basGatewayFrontendHost + s"/bas-gateway/sign-out-without-state?continue=$continueUrl"

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val cacheTtl: Long                  = configuration.get[Int]("mongodb.timeToLiveInSeconds")
  val individualDetailsCacheTtl: Long = configuration.get[Int]("mongodb.individualDetailsTtlInSeconds")

  val pdvBaseUrl: String                        = configuration.get[Service]("microservice.services.personal-details-validation").baseUrl
  val personalDetailsValidationFrontEnd: String =
    configuration.get[Service]("microservice.services.personal-details-validation-frontend").baseUrl

  val printAndPostServiceUrl: String =
    configuration.get[String]("external-url.national-insurance-number-letter-frontend.host")

  val fmnGuidancePageUrl: String = configuration.get[String]("urls.fmnGuidancePage")

  val hmrcExtraSupportUrl: String = configuration.get[String]("urls.hmrcExtraSupport")
  val callChargesUrl: String      = configuration.get[String]("urls.callCharges")

  def individualDetails: DesApiServiceConfig =
    DesApiServiceConfig(configuration.get[Configuration]("microservice.services.individual-details"))

  val individualDetailsProtocol: String = configuration.get[String]("external-url.individual-details.protocol")
  val individualDetailsHost: String     = configuration.get[String]("external-url.individual-details.host")
  val individualDetailsBaseUrl: String  = configuration.get[String]("external-url.individual-details.base-url")
  val individualDetailsPort: String     = configuration.get[String]("external-url.individual-details.port")

  val individualDetailsServiceUrl: String =
    s"$individualDetailsProtocol://$individualDetailsHost:$individualDetailsPort$individualDetailsBaseUrl"

  val npsFMNAPIProtocol: String     = configuration.get[String]("external-url.nps-fmn-api.protocol")
  val npsFMNAPIHost: String         = configuration.get[String]("external-url.nps-fmn-api.host")
  val npsFMNAPIBaseUrl: String      = configuration.get[String]("external-url.nps-fmn-api.base-url")
  val npsFMNAPIPort: String         = configuration.get[String]("external-url.nps-fmn-api.port")
  val npsFMNAPIOriginatorId: String = configuration.get[String]("external-url.nps-fmn-api.gov-uk-originator-id")

  val npsFMNAPIUrl: String = s"$npsFMNAPIProtocol://$npsFMNAPIHost:$npsFMNAPIPort$npsFMNAPIBaseUrl"

  // banners
  val showAlphaBanner: Boolean       = configuration.get[Boolean]("sca-wrapper.banners.show-alpha")
  val showBetaBanner: Boolean        = configuration.get[Boolean]("sca-wrapper.banners.show-beta")
  val showHelpImproveBanner: Boolean = configuration.get[Boolean]("sca-wrapper.banners.show-help-improve")

  val npsFMNAppStatusMessageList: String = configuration.get[String]("npsfmn.app-status-message-list")
  val encryptionKey: String              = configuration.get[String]("mongodb.encryption.key")
  val trustedDomains: Set[String]        = configuration.get[Seq[String]]("mdtp.trustedDomains").toSet
}
