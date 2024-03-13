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

import com.google.inject.ImplementedBy
import models.individualdetails.AccountStatusType.FullLive
import models.individualdetails.AddressStatus.NotDlo
import models.individualdetails.AddressType.ResidentialAddress
import models.individualdetails.CrnIndicator.{False, True}
import models.individualdetails.{Address, AddressList, IndividualDetails}
import play.api.Logging
import util.FMNConstants.EmptyString

import javax.inject.Inject
import scala.concurrent.ExecutionContext

@ImplementedBy(classOf[CheckDetailsServiceImpl])
trait CheckDetailsService {
  def checkConditions(idData: IndividualDetails): (Boolean, String)

}

class CheckDetailsServiceImpl @Inject()(implicit ec: ExecutionContext) extends CheckDetailsService with Logging {

   def checkConditions(idData: IndividualDetails): (Boolean, String) = {
     var reason = EmptyString
     val isValidNino:Boolean = idData.crnIndicator.equals(False)
     val isValidAccountStatus:Boolean = idData.accountStatusType.exists(_.equals(FullLive))
     val isValidAddressStatus:Boolean =  getAddressTypeResidential(idData.addressList).addressStatus match {
       case Some(NotDlo) => true
       case None => true
       case _ => false
     }

     if (!isValidAccountStatus) reason += "AccountStatus is not FullLive;"
     if (!isValidNino) reason += "CRN;"
     if (!isValidAddressStatus) reason += "AddressStatus is Dlo or NFa;"

    val isValidCustomer = isValidAccountStatus && isValidNino && isValidAddressStatus

    (isValidCustomer, reason)
  }

  private def getAddressTypeResidential(addressList: AddressList): Address = {
    val residentialAddress = addressList.getAddress.filter(_.addressType.equals(ResidentialAddress))
    residentialAddress.head
  }

}
