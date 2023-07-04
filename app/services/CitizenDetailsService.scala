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

package services

import connectors.CitizenDetailsConnector
import models.{PersonDetailsResponse, PersonDetailsSuccessResponse}
import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import com.google.inject.ImplementedBy

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[CitizenDetailsServiceImpl])
trait CitizenDetailsService {

  def getPersonalDetails(nino: String)(implicit request: RequestHeader, hc: HeaderCarrier): Future[PersonDetailsResponse]

  def getPostcode(nino: String)(implicit request: RequestHeader, hc: HeaderCarrier): Future[String]

}

class CitizenDetailsServiceImpl @Inject()(connector: CitizenDetailsConnector
  )(implicit val ec: ExecutionContext) extends CitizenDetailsService with Logging {

  private val emptyString: String = ""

  def getPersonalDetails(nino: String)(implicit request: RequestHeader, hc: HeaderCarrier): Future[PersonDetailsResponse] =
    connector.personDetails(Nino(nino))

  def getPostcode(nino: String)(implicit request: RequestHeader, hc: HeaderCarrier): Future[String] =
    for {
      personDetails <- getPersonalDetails(nino)
      postCode   =   personDetails match {
              case PersonDetailsSuccessResponse(pd) => pd.address.map(_.postcode.get).getOrElse(emptyString)
              case _                   => emptyString
            }
    } yield postCode

}