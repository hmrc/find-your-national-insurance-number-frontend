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

package config

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import controllers.actions._
import repositories.{EncryptedIndividualDetailsRepository, EncryptedPersonalDetailsValidationRepository,
  IndividualDetailsRepoTrait, IndividualDetailsRepository,
  PersonalDetailsValidationRepoTrait, PersonalDetailsValidationRepository}
import views.html.templates.{LayoutProvider, NewLayoutProvider}

import java.time.{Clock, ZoneOffset}

// $COVERAGE-OFF$
class Module(environment: Environment, config: Configuration) extends AbstractModule {

  private val encryptionEnabled   = config.get[Boolean]("mongodb.encryption.enabled")
  private val cl50Toggle: Boolean = config.get[Boolean]("features.cl50")

  override def configure(): Unit = {

    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()

    if(cl50Toggle) {
      bind(classOf[CL50DataRequiredAction]).to(classOf[CL50DataRequiredActionImpl]).asEagerSingleton()
    } else {
      bind(classOf[CL50DataRequiredAction]).to(classOf[JourneyClosedActionImpl]).asEagerSingleton()
    }

    bind(classOf[ValidCustomerDataRequiredAction]).to(classOf[ValidCustomerDataRequiredActionImpl]).asEagerSingleton()

    // For session based storage instead of cred based, change to SessionIdentifierAction
    bind(classOf[IdentifierAction]).to(classOf[SessionIdentifierAction]).asEagerSingleton()

    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))

    bind(classOf[LayoutProvider]).to(classOf[NewLayoutProvider]).asEagerSingleton()

    if (encryptionEnabled) {
      bind(classOf[IndividualDetailsRepoTrait])
        .to(classOf[EncryptedIndividualDetailsRepository]).asEagerSingleton()
      bind(classOf[PersonalDetailsValidationRepoTrait])
        .to(classOf[EncryptedPersonalDetailsValidationRepository]).asEagerSingleton()
    } else {
      bind(classOf[IndividualDetailsRepoTrait])
        .to(classOf[IndividualDetailsRepository]).asEagerSingleton()
      bind(classOf[PersonalDetailsValidationRepoTrait])
        .to(classOf[PersonalDetailsValidationRepository]).asEagerSingleton()
    }

  }
}
// $COVERAGE-ON$
