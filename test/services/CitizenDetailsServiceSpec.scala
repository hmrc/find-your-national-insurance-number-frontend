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

import base.SpecBase
import connectors.CitizenDetailsConnector
import models.{Address, Person, PersonDetails, PersonDetailsNotFoundResponse, PersonDetailsSuccessResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status._
import play.api.mvc.RequestHeader
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Random, Success}

class CitizenDetailsServiceSpec extends SpecBase {

  val mockConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val fakeNino = Nino(new Generator(new Random()).nextNino.nino)
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: RequestHeader = mock[RequestHeader]
  val cds: CitizenDetailsService = new CitizenDetailsServiceImpl(mockConnector)

  def buildFakeAddress: Address = Address(
    Some("1 Fake Street"),
    Some("Fake Town"),
    Some("Fake City"),
    Some("Fake Region"),
    None,
    Some("AA1 1AA"),
    None,
    Some(LocalDate.of(2015, 3, 15)),
    None,
    Some("Residential"),
    false
  )

  val buildPersonDetails: PersonDetails =
    PersonDetails(
      Person(
        Some("Firstname"),
        Some("Middlename"),
        Some("Lastname"),
        Some("FML"),
        Some("Dr"),
        Some("Phd."),
        Some("M"),
        Some(LocalDate.parse("1945-03-18")),
        Some(fakeNino)
      ),
      Some(buildFakeAddress),
      None
    )

  "CitizenDetailsService" - {
    "personDetails is called" - {
      "return person details when connector returns and OK status with body" in {
        when(mockConnector.personDetails(any())(any())).thenReturn(
          Future.successful(PersonDetailsSuccessResponse(buildPersonDetails))
        )
        val result = cds.getPersonalDetails(fakeNino.nino).value

        result.getOrElse(buildPersonDetails.copy(correspondenceAddress = None)) mustBe Success(PersonDetailsSuccessResponse(buildPersonDetails))
      }

      List(
        BAD_REQUEST,
        NOT_FOUND,
        TOO_MANY_REQUESTS,
        REQUEST_TIMEOUT,
        INTERNAL_SERVER_ERROR,
        SERVICE_UNAVAILABLE,
        BAD_GATEWAY
      ).foreach { errorResponse =>
        s"return an UpstreamErrorResponse containing $errorResponse when connector returns the same" in {
          when(mockConnector.personDetails(any())(any()))thenReturn(Future.successful(PersonDetailsNotFoundResponse))

          val result = cds.getPersonalDetails(fakeNino.nino).value
          result mustBe Some(Success(PersonDetailsNotFoundResponse))
        }
      }
    }
  }

}
