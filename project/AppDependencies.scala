import sbt.*

object AppDependencies {

  private val playVersion = "play-28"
  private val bootstrapVersion = "8.1.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping"      % s"1.12.0-$playVersion",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"           % "0.71.0",
    "uk.gov.hmrc"       %% s"sca-wrapper-$playVersion"          % "1.3.0",
    "org.typelevel"     %% "cats-core"                          % "2.7.0",
    "uk.gov.hmrc"       %% "crypto-json-play-28"                % "7.6.0"
)

  val test = Seq(
    "org.scalatestplus"    %% "mockito-4-6"                    % "3.2.15.0",
    "org.mockito"          %% "mockito-scala"                  % "1.16.42",
    "org.scalatestplus"    %% "scalacheck-1-17"                % "3.2.15.0",
    "uk.gov.hmrc"          %% s"bootstrap-test-$playVersion"   % "8.1.0",
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-test-$playVersion"  % "0.71.0",
    "com.vladsch.flexmark" % "flexmark-all"                    % "0.62.2"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
