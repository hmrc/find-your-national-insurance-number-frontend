package pages

import models.UpliftOrLetter
import pages.behaviours.PageBehaviours

class UpliftOrLetterPageSpec extends PageBehaviours {

  "UpliftOrLetterPage" - {

    beRetrievable[Set[UpliftOrLetter]](UpliftOrLetterPage)

    beSettable[Set[UpliftOrLetter]](UpliftOrLetterPage)

    beRemovable[Set[UpliftOrLetter]](UpliftOrLetterPage)
  }
}
