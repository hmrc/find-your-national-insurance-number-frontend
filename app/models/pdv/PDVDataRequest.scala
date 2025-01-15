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

package models.pdv

import models.UserAnswers
import play.api.mvc.{Request, WrappedRequest}

trait BasePDVDataRequest[A] extends WrappedRequest[A] {
  def userId: String
  def pdvResponse: PDVResponse
  def credId: Option[String]
}

case class PDVDataRequestWithOptionalUserAnswers[A](
                                                     request: Request[A],
                                                     userId: String,
                                                     pdvResponse: PDVResponse,
                                                     credId: Option[String],
                                                     userAnswers: Option[UserAnswers]
                                                   ) extends WrappedRequest[A](request) with BasePDVDataRequest[A]

case class PDVDataRequestWithUserAnswers[A](
                                             request: Request[A],
                                             userId: String,
                                             pdvResponse: PDVResponse,
                                             credId: Option[String],
                                             userAnswers: UserAnswers
                                           ) extends WrappedRequest[A](request) with BasePDVDataRequest[A]
