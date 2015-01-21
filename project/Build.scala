import sbt.Keys._
import sbt._

object Build extends sbt.Build {

  val AKKA_VERSION = "2.3.8"
  val SPRAY_VERSION = "1.3.1"
  val CONFIG_VERSION = "1.2.1"
  val CODAHALE_VERSION = "3.0.2"
  val LIFT_VERSION = "2.5.1"

  lazy val GeneralSettings = Seq[Def.Setting[_]](

    compile <<= (compile in Compile) dependsOn (compile in sbt.Test),

    /* basic project info */
    name := "service-container",

    organization := "com.github.vonnagy",

    version := "1.0.0-SNAPSHOT",

    description := "Service Container",

    scalaVersion := "2.10.4",

    scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),

    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),

    logLevel := Level.Debug,

    libraryDependencies ++= {
      Seq(
        "io.spray" % "spray-can" % SPRAY_VERSION,
        "io.spray" % "spray-http" % SPRAY_VERSION,
        "io.spray" % "spray-httpx" % SPRAY_VERSION,
        "io.spray" % "spray-routing" % SPRAY_VERSION,
        "com.typesafe" % "config" % CONFIG_VERSION,
        "com.typesafe.akka" %% "akka-actor" % AKKA_VERSION,
        "com.typesafe.akka" %% "akka-slf4j" % AKKA_VERSION,
        "org.slf4j" % "slf4j-api" % "1.7.5",
        "ch.qos.logback" % "logback-classic" % "1.0.13",
        "org.slf4j" % "log4j-over-slf4j" % "1.7.5",
        "net.liftweb" %% "lift-json" % LIFT_VERSION,
        "net.liftweb" %% "lift-json-ext" % LIFT_VERSION,
        "com.codahale.metrics" % "metrics-core" % CODAHALE_VERSION,
        "com.codahale.metrics" % "metrics-jvm" % CODAHALE_VERSION,
        "joda-time" % "joda-time" % "2.6",
        "com.typesafe.akka" %% "akka-testkit" % AKKA_VERSION % "test",
        "io.spray" % "spray-testkit" % SPRAY_VERSION % "test",
        "junit" % "junit" % "4.12" % "test",
        "org.specs2" %% "specs2-core" % "2.4.15" % "test",
        "org.scalamock" %% "scalamock-specs2-support" % "3.2.1" % "test" exclude("org.specs2", "specs2"),
        "com.novocode" % "junit-interface" % "0.11" % "test"
      )
    }
  )

  // Join the settings together
  val ServiceContainerSettings = GeneralSettings ++ Publish.settings ++ Test.settings

  val ExampleSettings = GeneralSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.sclasen" %% "akka-kafka" % "0.0.10")
  )

  val MetricsReportingSettings = GeneralSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.github.jjagged" % "metrics-statsd" % "1.0.0")
   )

  val noPublishing = Seq(
    publish := (),
    publishLocal := (),
    // required until these tickets are closed https://github.com/sbt/sbt-pgp/issues/42,
    // https://github.com/sbt/sbt-pgp/issues/36
    publishTo := None
  )

  lazy val BaseProject = Project(id="base", base=file("."))
    .settings(noPublishing:_*)
    .dependsOn(ServiceContainerProject)
    .aggregate(ServiceContainerProject, ServiceContainerMetricsReportingProject, ServiceContainerExamplesProject)

  lazy val ServiceContainerProject: Project = Project(id = "service-container", base = file("service-container"))
    .settings(ServiceContainerSettings: _*)

  lazy val ServiceContainerMetricsReportingProject: Project = Project(id = "service-container-metrics-reporting", base = file("service-container-metrics-reporting"))
    .settings(MetricsReportingSettings:_*)
    .dependsOn(ServiceContainerProject)

  lazy val ServiceContainerExamplesProject: Project = Project(id = "service-container-examples", base = file("service-container-examples"))
    .settings(noPublishing:_*)
    .settings(ExampleSettings:_*)
    .dependsOn(ServiceContainerProject)


}