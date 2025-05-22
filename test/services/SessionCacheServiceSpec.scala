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

package services

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify}
import org.scalatest.BeforeAndAfterEach
import repositories._
import services.NPSFMNServiceSpec.fakeNino

class SessionCacheServiceSpec extends SpecBase with BeforeAndAfterEach {

  val mockPersonalDetailsValidationRepository: PersonalDetailsValidationRepository =
    mock[PersonalDetailsValidationRepository]
  val mockIndividualDetailsRepository: IndividualDetailsRepository                 =
    mock[IndividualDetailsRepository]

  val mockEncPersonalDetailsValidationRepository: EncryptedPersonalDetailsValidationRepository =
    mock[EncryptedPersonalDetailsValidationRepository]
  val mockEncIndividualDetailsRepository: EncryptedIndividualDetailsRepository                 =
    mock[EncryptedIndividualDetailsRepository]

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  override def beforeEach(): Unit =
    reset(
      mockSessionRepository,
      mockPersonalDetailsValidationRepository,
      mockPersonalDetailsValidationRepository,
      mockEncPersonalDetailsValidationRepository,
      mockEncIndividualDetailsRepository
    )

  val service = new SessionCacheService(
    mockSessionRepository,
    mockIndividualDetailsRepository,
    mockPersonalDetailsValidationRepository
  )

  val serviceEnc = new SessionCacheService(
    mockSessionRepository,
    mockEncIndividualDetailsRepository,
    mockEncPersonalDetailsValidationRepository
  )

  "SessionCacheService" - {

    "invalidateCache (non encrypted)" - {

      "delete from the cache" in {
        service.invalidateCache(fakeNino.nino, "id")
        verify(mockSessionRepository, times(1)).clear(any())
        verify(mockIndividualDetailsRepository, times(1)).clear(any())
        verify(mockPersonalDetailsValidationRepository, times(1)).clear(any())
      }

    }

    "invalidateCache (encrypted)" - {

      "delete from the cache" in {
        serviceEnc.invalidateCache(fakeNino.nino, "id")
        verify(mockSessionRepository, times(1)).clear(any())
        verify(mockEncIndividualDetailsRepository, times(1)).clear(any())
        verify(mockEncPersonalDetailsValidationRepository, times(1)).clear(any())
      }

    }
  }
}
