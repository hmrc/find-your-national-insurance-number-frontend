package pages

import pages.behaviours.PageBehaviours

class serviceIvEvidencePageSpec extends PageBehaviours {

  "serviceIvEvidencePage" - {

    beRetrievable[Boolean](ServiceIvEvidencePage)

    beSettable[Boolean](ServiceIvEvidencePage)

    beRemovable[Boolean](ServiceIvEvidencePage)
  }
}
