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

import repositories.{IndividualDetailsRepoTrait, PersonalDetailsValidationRepoTrait, SessionRepository}

import javax.inject.Inject
import scala.concurrent.Future

class SessionCacheService @Inject() (
  sessionRepository: SessionRepository,
  individualDetailsRepository: IndividualDetailsRepoTrait,
  personalDetailsValidationRepository: PersonalDetailsValidationRepoTrait
) {

  def invalidateCache(nino: String, userId: String): Future[Boolean] = {
    personalDetailsValidationRepository.clear(nino)
    individualDetailsRepository.clear(nino)
    sessionRepository.clear(userId)
  }

}
