import sbt.Keys._
import sbt._

object Build extends sbt.Build {

  val AKKA_VERSION = "2.3.8"
  val SPRAY_VERSION = "1.3.1"
  val CONFIG_VERSION = "1.2.1"
  val METRICS_VERSION = "3.1.0"
  val LIFT_VERSION = "2.5.1"

  lazy val commonSettings = Defaults.coreDefaultSettings ++ Seq(

    compile <<= (compile in Compile) dependsOn (compile in sbt.Test),

    organization := "com.github.vonnagy",

    version := "1.0.0",

    description := "Service Container",

    scalaVersion := "2.10.4",

    scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),

    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),

    logLevel := Level.Info,

    libraryDependencies ++= {
      Seq(
        "io.spray"              % "spray-can"         % SPRAY_VERSION,
        "io.spray"              % "spray-http"        % SPRAY_VERSION,
        "io.spray"              % "spray-httpx"       % SPRAY_VERSION,
        "io.spray"              % "spray-routing"     % SPRAY_VERSION,
        "com.typesafe"          % "config"            % CONFIG_VERSION,
        "com.typesafe.akka"     %% "akka-actor"       % AKKA_VERSION exclude ("org.scala-lang" , "scala-library"),
        "com.typesafe.akka"     %% "akka-slf4j"       % AKKA_VERSION exclude ("org.slf4j", "slf4j-api") exclude ("org.scala-lang" , "scala-library"),
        "org.slf4j"             % "slf4j-api"         % "1.7.5",
        "ch.qos.logback"        % "logback-classic"   % "1.0.13",
        "org.slf4j"             % "log4j-over-slf4j"  % "1.7.5",
        "net.liftweb"           %% "lift-json"        % LIFT_VERSION,
        "net.liftweb"           %% "lift-json-ext"    % LIFT_VERSION exclude ("org.scala-lang" , "scala-compiler"),
        "io.dropwizard.metrics" % "metrics-core"      % METRICS_VERSION,
        "io.dropwizard.metrics" % "metrics-jvm"       % METRICS_VERSION,
        "joda-time"             % "joda-time"         % "2.6",
        "com.typesafe.akka"     %% "akka-testkit"     % AKKA_VERSION    % "test",
        "io.spray"              % "spray-testkit"     % SPRAY_VERSION   % "test",
        "junit"                 % "junit"             % "4.12"          % "test",
        "org.specs2"            %% "specs2-core"      % "2.4.15"        % "test",
        "org.scalamock"         %% "scalamock-specs2-support" % "3.2.1" % "test" exclude("org.specs2", "specs2")
      )
    }
  )

  // Join the settings together
  val standardSettings = commonSettings ++ Publish.settings ++ Test.settings

  val exampleSettings = Seq(
    name := "service-container-examples",
    libraryDependencies ++= Seq(
      "com.sclasen" %% "akka-kafka" % "0.0.10")
  ) ++ Test.settings

  val metricsReportingSettings = Seq(
    name := "service-container-metrics-reporting",
    libraryDependencies ++= Seq(
      "com.github.jjagged" % "metrics-statsd" % "1.0.0" exclude("com.codahale.metrics", "metrics"),
      "com.novaquark" % "metrics-influxdb" % "0.3.0" exclude("com.codahale.metrics", "metrics"))
   )

  val noPublishing = Seq(
    publish := (),
    publishLocal := (),
    // required until these tickets are closed https://github.com/sbt/sbt-pgp/issues/42,
    // https://github.com/sbt/sbt-pgp/issues/36
    publishTo := None
  )

  lazy val root = Project(id="root", base=file("."))
    .settings(noPublishing:_*)
    .aggregate(core, metricsReporting, examples)

  lazy val core = Project(id = "service-container", base = file("service-container"))
    .settings(standardSettings: _*)
    .settings(name := "service-container")

  lazy val metricsReporting = Project(id = "service-container-metrics-reporting", base = file("service-container-metrics-reporting"))
    .settings(standardSettings:_*)
    .settings(metricsReportingSettings:_*)
    .dependsOn(core)

  lazy val examples: Project = Project(id = "service-container-examples", base = file("service-container-examples"))
    .settings(noPublishing:_*)
    .settings(commonSettings:_*)
    .settings(exampleSettings:_*)
    .dependsOn(core)


}