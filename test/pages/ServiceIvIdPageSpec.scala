package pages

import pages.behaviours.PageBehaviours

class ServiceIvIdPageSpec extends PageBehaviours {

  "ServiceIvIdPage" - {

    beRetrievable[Boolean](ServiceIvIdPage)

    beSettable[Boolean](ServiceIvIdPage)

    beRemovable[Boolean](ServiceIvIdPage)
  }
}
