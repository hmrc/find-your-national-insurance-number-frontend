import sbt.*

object AppDependencies {

  private val playVersion = "play-29"
  private val bootstrapVersion = "8.5.0"
  private val mongoVersion = "1.8.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% s"play-conditional-form-mapping-$playVersion"  % "2.0.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion"           % mongoVersion,
    "uk.gov.hmrc"       %% s"sca-wrapper-$playVersion"          % "1.5.0",
    "org.typelevel"     %% "cats-core"                          % "2.10.0",
    "uk.gov.hmrc"       %% s"crypto-json-$playVersion"          % "7.6.0"
)

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus"    %% "mockito-4-11"                   % "3.2.18.0",
    "org.mockito"          %% "mockito-scala"                  % "1.17.30",
    "org.scalatestplus"    %% "scalacheck-1-17"                % "3.2.16.0",
    "uk.gov.hmrc"          %% s"bootstrap-test-$playVersion"   % bootstrapVersion,
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-test-$playVersion"  % mongoVersion,
    "com.vladsch.flexmark" % "flexmark-all"                    % "0.64.8",
    "org.scalamock"        %% "scalamock"                      % "5.2.0"
  ).map(_ % "test")

  def apply(): Seq[ModuleID] = compile ++ test
}
