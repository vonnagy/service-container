import sbt.Keys._
import sbt._

object Build extends sbt.Build {

  val AKKA_VERSION = "2.3.11"
  val SPRAY_VERSION = "1.3.3"
  val CONFIG_VERSION = "1.3.0"
  val METRICS_VERSION = "3.1.2"
  val LIFT_VERSION = "2.6.2"

  lazy val buildSettings = Seq(
    name := "Service Container",
    organization := "com.github.vonnagy",
    version := "1.0.2-SNAPSHOT",
    description := "Service Container",
    scalaVersion := "2.11.6",
    crossScalaVersions := Seq("2.10.5", "2.11.6")
  )

  override lazy val settings = super.settings ++ buildSettings

  lazy val parentSettings = buildSettings ++ Seq(
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    // required until these tickets are closed https://github.com/sbt/sbt-pgp/issues/42,
    // https://github.com/sbt/sbt-pgp/issues/36
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )

  lazy val defaultSettings = Seq(

    compile <<= (compile in Compile) dependsOn (compile in sbt.Test),

    scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),

    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),

    logLevel := Level.Info,

    resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases"

  )

  lazy val dependencySettings = Seq(

    libraryDependencies ++= {
      Seq(
        "io.spray"              %%  "spray-can"         % SPRAY_VERSION,
        "io.spray"              %%  "spray-http"        % SPRAY_VERSION,
        "io.spray"              %%  "spray-httpx"       % SPRAY_VERSION,
        "io.spray"              %%  "spray-routing"     % SPRAY_VERSION,
        "com.typesafe"          %   "config"            % CONFIG_VERSION,
        "com.typesafe.akka"     %%  "akka-actor"        % AKKA_VERSION exclude ("org.scala-lang" , "scala-library"),
        "com.typesafe.akka"     %%  "akka-slf4j"        % AKKA_VERSION exclude ("org.slf4j", "slf4j-api") exclude ("org.scala-lang" , "scala-library"),
        "com.typesafe.akka"     %%  "akka-remote"       % AKKA_VERSION exclude ("org.slf4j", "slf4j-api") exclude ("org.scala-lang" , "scala-library"),
        "org.slf4j"             %   "slf4j-api"         % "1.7.12",
        "ch.qos.logback"        %   "logback-classic"   % "1.1.3",
        "org.slf4j"             %   "log4j-over-slf4j"  % "1.7.12",
        "net.liftweb"           %%  "lift-json"         % LIFT_VERSION,
        "net.liftweb"           %%  "lift-json-ext"     % LIFT_VERSION exclude ("org.scala-lang" , "scala-compiler"),
        "io.dropwizard.metrics" %   "metrics-core"      % METRICS_VERSION,
        "io.dropwizard.metrics" %   "metrics-jvm"       % METRICS_VERSION,
        "joda-time"             %   "joda-time"         % "2.8.1",
        "com.typesafe.akka"     %%  "akka-testkit"      % AKKA_VERSION    % "test",
        "io.spray"              %%  "spray-testkit"     % SPRAY_VERSION   % "test",
        "junit"                 %   "junit"             % "4.12"          % "test",
        "org.scalaz.stream"     %%  "scalaz-stream"     % "0.7a"          % "test",
        "org.specs2"            %%  "specs2-core"       % "3.6.1"         % "test",
        "org.specs2"            %%  "specs2-mock"       % "3.6.1"         % "test"
      )
    }
  )

  val standardSettings = defaultSettings ++ dependencySettings ++ Publish.settings ++ Test.settings

  val exampleSettings = Seq(
    name := "service-container-examples",
    libraryDependencies ++= Seq(
      "com.sclasen" %% "akka-kafka" % "0.1.0")
  ) ++ Test.settings

  val metricsReportingSettings = Seq(
    name := "service-container-metrics-reporting",
    libraryDependencies ++= Seq(
      "com.github.jjagged" % "metrics-statsd" % "1.0.0" exclude("com.codahale.metrics", "metrics"),
      "com.novaquark" % "metrics-influxdb" % "0.3.0" exclude("com.codahale.metrics", "metrics"))
  )

  lazy val root = Project(id = "root", base = file("."))
    .settings(parentSettings)
    .aggregate(core, metricsReporting, examples)


  lazy val core = Project(id = "service-container", base = file("service-container"))
    .settings(standardSettings)
    .settings(name := "service-container")

  lazy val metricsReporting = Project(id = "service-container-metrics-reporting", base = file("service-container-metrics-reporting"))
    .settings(standardSettings)
    .settings(metricsReportingSettings: _*)
    .dependsOn(core)

  lazy val examples: Project = Project(id = "service-container-examples", base = file("service-container-examples"))
    .settings(defaultSettings: _*)
    .settings(dependencySettings: _*)
    .settings(exampleSettings: _*)
    .dependsOn(core)


}