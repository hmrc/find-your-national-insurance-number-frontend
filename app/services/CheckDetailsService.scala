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
import models.individualdetails.CrnIndicator.False
import models.individualdetails.{Address, AddressList, IndividualDetails}
import org.apache.commons.lang3.StringUtils
import play.api.Logging

import javax.inject.Inject

@ImplementedBy(classOf[CheckDetailsServiceImpl])
trait CheckDetailsService {
  def checkConditions(idData: IndividualDetails): (Boolean, String)

}

class CheckDetailsServiceImpl @Inject() extends CheckDetailsService with Logging {

   def checkConditions(idData: IndividualDetails): (Boolean, String) = {
     var reason: String                 = StringUtils.EMPTY
     val isValidNino:Boolean            = idData.crnIndicator.equals(False)
     val isValidAccountStatus:Boolean   = idData.accountStatusType.exists(_.equals(FullLive))
     val hasResidentialAddress: Boolean = idData.getAddressTypeResidential.isDefined
     val validStatus: Boolean           = isValidAddressStatus(idData)

     if (!isValidAccountStatus) reason += "AccountStatus is not FullLive;"
     if (!isValidNino) reason += "CRN;"
     if (!hasResidentialAddress) reason += "No residential address;"
     if (!validStatus && hasResidentialAddress) reason += "AddressStatus is Dlo or NFa;"

    val isValidCustomer = isValidAccountStatus && isValidNino && validStatus && hasResidentialAddress
    (isValidCustomer, reason)
  }

  private def isValidAddressStatus(idData: IndividualDetails): Boolean = {
    getAddressTypeResidential(idData.addressList).exists(
      status => status.addressStatus.isEmpty || status.addressStatus.get.equals(NotDlo)
    )
  }

  private def getAddressTypeResidential(addressList: AddressList): Option[Address] = {
    val residentialAddress = addressList.getAddress.filter(_.addressType.equals(ResidentialAddress))
    residentialAddress.headOption
  }

}
