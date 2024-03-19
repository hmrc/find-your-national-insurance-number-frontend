package models

import generators.ModelGenerators
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class UpliftOrLetterSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues with ModelGenerators {

  "UpliftOrLetter" - {

    "must deserialise valid values" in {

      val gen = arbitrary[UpliftOrLetter]

      forAll(gen) {
        upliftOrLetter =>

          JsString(upliftOrLetter.toString).validate[UpliftOrLetter].asOpt.value mustEqual upliftOrLetter
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!UpliftOrLetter.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[UpliftOrLetter] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = arbitrary[UpliftOrLetter]

      forAll(gen) {
        upliftOrLetter =>

          Json.toJson(upliftOrLetter) mustEqual JsString(upliftOrLetter.toString)
      }
    }
  }
}
