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

package models.pdv

sealed trait ValidationStatus

object ValidationStatus {
  case object Success extends ValidationStatus {
    override def toString: String = "success"
  }
  case object Failure extends ValidationStatus {
    override def toString: String = "failure"
  }
  case object Unknown extends ValidationStatus {
    override def toString: String = ""
  }

  def fromString(value: String): ValidationStatus = value.toLowerCase match {
    case "success" => Success
    case "failure" => Failure
    case _ => Unknown
  }

  def withName(name: String): ValidationStatus = name.toLowerCase match {
    case "success" => Success
    case "failure" => Failure
    case _ => throw new IllegalArgumentException(s"Invalid name for ValidationStatus: $name")
  }
}
