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

import com.google.inject.ImplementedBy
import connectors.NPSFMNConnector
import models.nps.NPSFMNRequest
import play.api.Logging
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[NPSFMNServiceImpl])
trait NPSFMNService {
  def updateDetails(nino: String, npsFMNRequest: NPSFMNRequest
                   )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue]
}

class NPSFMNServiceImpl @Inject()(connector: NPSFMNConnector)(implicit val ec: ExecutionContext)
  extends NPSFMNService with Logging {

  def updateDetails(nino: String, npsFMNRequest: NPSFMNRequest
                   )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsValue] = {
    val identifier = nino.substring(0, nino.length-1)
    connector.updateDetails(identifier, npsFMNRequest)
  }

}
