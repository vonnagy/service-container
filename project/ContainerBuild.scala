import sbt.Keys._
import sbt._

object ContainerBuild extends Build {

  val SCALA_VERSION = "2.11.8"
  val JDK = "1.8"

  val AKKA_VERSION    = "2.4.16"
  val AKKA_HTTP_VERSION = "10.0.3"
  val AKKA_SSL_VERSION = "0.2.1"
  val CONFIG_VERSION  = "1.3.0"
  val JODA_VERSION    = "2.9.4"
  val JSON4S_VERSION  = "3.4.1"
  val LOGBACK_VERSION = "1.1.7"
  val METRICS_VERSION = "3.1.2"
  val SLF4J_VERSION   = "1.7.21"
  val SPECS_VERSION   = "3.8.5-20161006092037-b43b121"

  val buildNumber = sys.env.get("BUILD_NUMBER").getOrElse("000")

  lazy val baseSettings = Seq(
    name := "Service Container",
    organization := "com.github.vonnagy",
    version := "2.0.1",
    description := "Service Container",
    scalaVersion := SCALA_VERSION,
    packageOptions in (Compile, packageBin) +=
        Package.ManifestAttributes( "Implementation-Build" -> buildNumber )
  )

  override val settings = super.settings ++ baseSettings

  lazy val noPublishSettings = Seq(
    publish := {},
    publishLocal := {},
    publishArtifact := false,
    // required until these tickets are closed https://github.com/sbt/sbt-pgp/issues/42,
    // https://github.com/sbt/sbt-pgp/issues/36
    publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
  )

  lazy val defaultSettings = Test.testSettings ++ Seq(

    logLevel := Level.Info,

    scalacOptions ++= Seq("-encoding", "UTF-8", "-feature", "-language:_", "-deprecation", "-unchecked"),
    javacOptions in Compile ++= Seq("-encoding", "UTF-8", "-source", JDK, "-target", JDK,
      "-Xlint:unchecked", "-Xlint:deprecation", "-Xlint:-options"),

    run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)),
    runMain in Compile <<= Defaults.runMainTask(fullClasspath in Compile, runner in (Compile, run)),

    resolvers += "Scalaz Bintray Repo" at "https://dl.bintray.com/scalaz/releases",

    ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,
    ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },

    parallelExecution in ThisBuild := false,
    parallelExecution in Global := false
  )

  object Dependencies {

    object Compile {
      val config          = "com.typesafe"          %   "config"            % CONFIG_VERSION
      val akkaActor       = "com.typesafe.akka"     %%  "akka-actor"        % AKKA_VERSION
      val akkaHttp        = "com.typesafe.akka"     %%  "akka-http-core"    % AKKA_HTTP_VERSION
      val akkaHttpExp     = "com.typesafe.akka"     %%  "akka-http" % AKKA_HTTP_VERSION
      val akkaSlf4j       = "com.typesafe.akka"     %%  "akka-slf4j"        % AKKA_VERSION
      val akkaRemote      = "com.typesafe.akka"     %%  "akka-remote"       % AKKA_VERSION
      val akkaSSL         = "com.typesafe"          %%  "ssl-config-akka"   % AKKA_SSL_VERSION
      val slf4j           = "org.slf4j"             %   "slf4j-api"         % SLF4J_VERSION
      val logback         = "ch.qos.logback"        %   "logback-classic"   % LOGBACK_VERSION
      val slf4jOverLog4j  = "org.slf4j"             %   "log4j-over-slf4j"  % SLF4J_VERSION
      val json4sJackson   = "org.json4s"            %%  "json4s-jackson"    % JSON4S_VERSION
      val json4sExt       = "org.json4s"            %%  "json4s-ext"        % JSON4S_VERSION
      val metricsCore     = "io.dropwizard.metrics" %   "metrics-core"      % METRICS_VERSION
      val metricsJvm      = "io.dropwizard.metrics" %   "metrics-jvm"       % METRICS_VERSION
      val joda            = "joda-time"             %   "joda-time"         % JODA_VERSION

      val akkaKafka       = "com.sclasen"           %%  "akka-kafka"        % "0.1.0"
      val metricsStatsd   = "com.github.jjagged"    %   "metrics-statsd"    % "1.0.0"
      val metricsDataDog  = "org.coursera"          %   "metrics-datadog"   % "1.1.3"
    }
    
    object Test {
      val akkaTest        = "com.typesafe.akka"     %%  "akka-testkit"      % AKKA_VERSION  % "test"
      val akkaHttpTest    = "com.typesafe.akka"     %%  "akka-http-testkit" % AKKA_HTTP_VERSION  % "test"
      val specsCore       = "org.specs2"            %%  "specs2-core"       % SPECS_VERSION % "test"
      val specsMock       = "org.specs2"            %%  "specs2-mock"       % SPECS_VERSION % "test"
      val scalazStream    = "org.scalaz.stream"     %%  "scalaz-stream"     % "0.7a"        % "test"
    }

    import Dependencies.Compile._
    import Dependencies.Test._

    val akka = Seq(akkaActor, akkaHttp, akkaHttpExp, akkaRemote, akkaSlf4j, akkaSSL)
    val json = Seq(json4sJackson, json4sExt)
    val logging = Seq(logback, slf4j, slf4jOverLog4j)
    val metrics = Seq(metricsCore, metricsJvm)

    val base = akka ++ json ++ logging ++ metrics ++ Seq(joda)
    val test = Seq(akkaTest, akkaHttpTest, specsCore, specsMock, scalazStream)

    val core = base ++ test
    val reporting = test ++ Seq(metricsStatsd, metricsDataDog)
    val examples = Seq(akkaKafka)

    val overrrides = Set(joda, metricsCore, slf4j)
  }

  lazy val moduleSettings = defaultSettings ++ Publish.settings

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = noPublishSettings ++ baseSettings,
    aggregate = Seq(container, metricsReporting, examples)
  )

  lazy val container = Project(
    id = "service-container",
    base = file("./service-container"),
    settings = moduleSettings
        ++ Set(dependencyOverrides ++= Dependencies.overrrides)
        ++ Seq(libraryDependencies ++= Dependencies.core)
  )

  lazy val metricsReporting = Project(
    id = "service-container-metrics-reporting",
    base = file("./service-container-metrics-reporting"),
    settings = moduleSettings
        ++ Set(dependencyOverrides ++= Dependencies.overrrides)
        ++ Seq(libraryDependencies ++= Dependencies.reporting)
  ).dependsOn(container % "test->test;compile->compile")

  lazy val examples = Project(
    id = "service-container-examples",
    base = file("./service-container-examples"),
    settings = noPublishSettings ++ defaultSettings
        ++ Set(dependencyOverrides ++= Dependencies.overrrides)
        ++ Seq(libraryDependencies ++= Dependencies.examples)
  ).dependsOn(container)

}
