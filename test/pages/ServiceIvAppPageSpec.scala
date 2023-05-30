package pages

import pages.behaviours.PageBehaviours

class ServiceIvAppPageSpec extends PageBehaviours {

  "ServiceIvAppPage" - {

    beRetrievable[Boolean](ServiceIvAppPage)

    beSettable[Boolean](ServiceIvAppPage)

    beRemovable[Boolean](ServiceIvAppPage)
  }
}
