package pages

import pages.behaviours.PageBehaviours

class PostLetterPageSpec extends PageBehaviours {

  "PostLetterPage" - {

    beRetrievable[Boolean](PostLetterPage)

    beSettable[Boolean](PostLetterPage)

    beRemovable[Boolean](PostLetterPage)
  }
}
