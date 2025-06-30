import sbt.*

object AppDependencies {

  private val playVersion = "play-30"
  private val bootstrapVersion = "9.12.0"
  private val mongoVersion = "2.6.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% s"play-conditional-form-mapping-$playVersion"  % "3.3.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"           % mongoVersion,
    "uk.gov.hmrc"       %% s"sca-wrapper-$playVersion"          % "2.10.0",
    "org.typelevel"     %% "cats-core"                          % "2.13.0",
    "uk.gov.hmrc"       %% s"crypto-json-$playVersion"          % "8.2.0"
)

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus"    %% "scalacheck-1-18"                % "3.2.19.0",
    "uk.gov.hmrc"          %% s"bootstrap-test-$playVersion"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-test-$playVersion"  % mongoVersion
  ).map(_ % "test")

  def apply(): Seq[ModuleID] = compile ++ test
}
