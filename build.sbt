import play.sbt.routes.RoutesKeys
import sbt.Def
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "find-your-national-insurance-number-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "3.3.6"
ThisBuild / scalafmtOnCompile := true

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedFiles := ";.*controllers.LanguageSwitchController.*;.*services.AuditService;.*connectors.HttpReadsWrapper",
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;view.*;config.*;.*(AuthService|BuildInfo|Routes).*;.*DataRetrievalActionImpl.*;models.Mode.*;models.encryption.*;.*\\$anon.*",
    ScoverageKeys.coverageMinimumStmtTotal := 88,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
  )
}

addCommandAlias("report", ";clean; coverage; test; it/test; coverageReport")

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings): _*)
  .settings(ThisBuild / useSuperShell := false)
  .settings(
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "views.ViewUtils._",
      "models.Mode",
      "controllers.routes._",
      "viewmodels.govuk.all._"
    ),
    PlayKeys.playDefaultPort := 14033,
    scalacOptions ++= Seq(
      "-unchecked",
      "-feature",
      "-language:noAutoTupling",
      "-Werror",
      "-Wconf:msg=unused import&src=.*views/.*:s",
      "-Wconf:msg=unused import&src=<empty>:s",
      "-Wconf:msg=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:msg=unused&src=.*Routes\\.scala:s",
      "-Wconf:msg=unused&src=.*ReverseRoutes\\.scala:s",
      "-Wconf:msg=unused&src=.*JavaScriptReverseRoutes\\.scala:s",
      "-Wconf:msg=Flag.*repeatedly:s",
      "-Wconf:msg=method safeSignoutUrl in class WrapperService is deprecated:s",
      "-Wconf:src=routes/.*:s"
    ),
    scoverageSettings,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    retrieveManaged := true,
    Global / excludeLintKeys += update / evictionWarningOptions,
    update / evictionWarningOptions :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    // concatenate js
    Concat.groups := Seq("javascripts/application.js" -> group(Seq("javascripts/app.js"))),
    pipelineStages := Seq(digest),
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(
    libraryDependencies ++= AppDependencies.test,
    DefaultBuildSettings.itSettings()
  )
