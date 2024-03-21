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

package Helpers

import org.joda.time.{DateTime, DateTimeZone, LocalDate}

import javax.inject.Inject

class TaxYearResolver @Inject() {
  lazy val now: () => DateTime = () => DateTime.now

  private val ukTime: DateTimeZone = DateTimeZone.forID("Europe/London")

  private def taxYearFor(dateToResolve: LocalDate): Int = {
    val year: Int  = dateToResolve.year.get
    val month: Int = 4
    val day: Int   = 6

    if (dateToResolve.isBefore(new LocalDate(year, month, day))) {
      year - 1
    } else {
      year
    }
  }

  def currentTaxYear: Int = taxYearFor(new LocalDate(now(), ukTime))
}
