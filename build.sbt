val CONTAINER_VERSION = "2.0.5"
val SCALA_VERSION = "2.12.1"
val JDK = "1.8"

val buildNumber = sys.env.get("BUILD_NUMBER").getOrElse("000")

lazy val baseSettings = Seq(
  name := "Service Container",
  organization := "com.github.vonnagy",
  version := CONTAINER_VERSION,
  description := "Service Container",
  crossScalaVersions := Seq("2.11.8", "2.12.1"),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor >= 11 =>
        Seq("-Ywarn-unused-import", "-Ywarn-unused")
      case _ =>
        Nil
    }
  },
  scalaVersion := SCALA_VERSION,
  packageOptions in(Compile, packageBin) +=
    Package.ManifestAttributes("Implementation-Build" -> buildNumber)
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  // required until these tickets are closed https://github.com/sbt/sbt-pgp/issues/42,
  // https://github.com/sbt/sbt-pgp/issues/36
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
)

lazy val defaultSettings = baseSettings ++ Seq(

  logLevel := Level.Info,

  scalacOptions ++= Seq("-encoding", "UTF-8", "-feature", "-language:_", "-deprecation", "-unchecked"),
  javacOptions in Compile ++= Seq("-encoding", "UTF-8", "-source", JDK, "-target", JDK,
    "-Xlint:unchecked", "-Xlint:deprecation", "-Xlint:-options"),

  run in Compile := Defaults.runTask(fullClasspath in Compile, mainClass in(Compile, run), runner in(Compile, run)),
  runMain in Compile := Defaults.runMainTask(fullClasspath in Compile, runner in(Compile, run)),

  resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",

  ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
  ivyScala := ivyScala.value map {
    _.copy(overrideScalaVersion = true)
  },

  parallelExecution in ThisBuild := false,
  parallelExecution in Global := false
)

lazy val moduleSettings = defaultSettings ++ Test.testSettings ++ Publish.settings

lazy val root = (project in file("."))
  .settings(defaultSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(name := "root")
  .aggregate(container, metricsReporting, examples)

lazy val container = (project in file("service-container"))
  .settings(moduleSettings: _*)
  .settings(
    name := "service-container",
    dependencyOverrides ++= Dependencies.overrrides,
    libraryDependencies ++= Dependencies.core
  )

lazy val metricsReporting = (project in file("service-container-metrics-reporting"))
  .settings(moduleSettings: _*)
  .settings(
    name := "service-container-metrics-reporting",
    dependencyOverrides ++= Dependencies.overrrides,
    libraryDependencies ++= Dependencies.reporting
  )
  .dependsOn(container % "test->test;compile->compile")

lazy val examples = (project in file("service-container-examples"))
  .settings(moduleSettings: _*)
  .settings(
    name := "service-container-examples",
    dependencyOverrides ++= Dependencies.overrrides,
    libraryDependencies ++= Dependencies.examples
  )
  .dependsOn(container)
