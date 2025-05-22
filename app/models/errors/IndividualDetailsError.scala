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

package models.errors

import models.IndividualDetailsIdentifier

sealed abstract class IndividualDetailsError(message: String) extends Throwable {
  val errorMessage: String = message
}

final case class ConnectorError(statusCode: Int, message: String) extends IndividualDetailsError(message)

final case class InvalidIdentifier(identifier: IndividualDetailsIdentifier)
    extends IndividualDetailsError(s"Invalid identifier: $identifier")
