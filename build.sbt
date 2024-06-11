import play.sbt.routes.RoutesKeys
import sbt.Def
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "find-your-national-insurance-number-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.13"

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;view.*;.*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
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
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
      "uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl"
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
    scalacOptions ++= Seq(
      "-feature",
      "-language:postfixOps",
      "-rootdir",
      baseDirectory.value.getCanonicalPath,
      "-Wconf:cat=deprecation:ws,cat=feature:ws,cat=optimizer:ws,src=target/.*:s"
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    retrieveManaged := true,
    update / evictionWarningOptions :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers ++= Seq(Resolver.jcenterRepo),
    // concatenate js
    Concat.groups := Seq(
      "javascripts/application.js" ->
        group(Seq(
          "javascripts/app.js"
        ))
    ),
    pipelineStages := Seq(digest),
  )
.settings(PlayKeys.playDefaultPort := 14033)
.settings(scoverageSettings: _*)

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
