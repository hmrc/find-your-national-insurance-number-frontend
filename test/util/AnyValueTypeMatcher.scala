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

package util

import models.CorrelationId
import models.individualdetails.ResolveMerge
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers.argThat

import java.util.UUID

object AnyValueTypeMatcher {

  implicit val defaultResolveMerge: Default[ResolveMerge] = new Default[ResolveMerge] {
    val getDefault: ResolveMerge = ResolveMerge('X')
  }
  implicit val defaultCorrelationId: Default[CorrelationId] = new Default[CorrelationId] {
    val getDefault: CorrelationId = CorrelationId(UUID.nameUUIDFromBytes(new Array[Byte](16)))
  }

  trait Default[T] {
    def getDefault: T
  }

  def anyValueType[T](implicit d: Default[T]): T = {
    argThat(new ArgumentMatcher[T] {
      def matches(argument: T): Boolean = true
    })
    d.getDefault
  }
}