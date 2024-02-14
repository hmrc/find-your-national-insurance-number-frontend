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

package repositories

import models.pdv.PDVResponseData

import scala.concurrent.Future

trait PersonalDetailsValidationRepoTrait {
  def insertOrReplacePDVResultData(pdvResponseData: PDVResponseData): Future[String]
  def updateCustomerValidityWithReason(nino: String, validCustomer: Boolean, reason: String): Future[String]
  def updatePDVDataWithNPSPostCode(nino: String, npsPostCode: String): Future[String]
  def findByNino(nino: String): Future[Option[PDVResponseData]]
}
