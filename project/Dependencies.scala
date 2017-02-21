import sbt._

object Dependencies {
  val AKKA_VERSION = "2.4.16"
  val AKKA_HTTP_VERSION = "10.0.3"
  val AKKA_SSL_VERSION = "0.2.1"
  val CONFIG_VERSION = "1.3.0"
  val SCALA_CONFIG_VERSION = "0.4.4"
  val JODA_VERSION = "2.9.4"
  val JSON4S_VERSION = "3.4.1"
  val LOGBACK_VERSION = "1.1.7"
  val METRICS_VERSION = "3.1.2"
  val SLF4J_VERSION = "1.7.21"
  val SPECS_VERSION = "3.8.5-20161006092037-b43b121"

  object CompileDep {
    val config = "com.typesafe" % "config" % CONFIG_VERSION
    val scalaConfig = "com.github.kxbmap" %% "configs" % SCALA_CONFIG_VERSION
    val akkaActor = "com.typesafe.akka" %% "akka-actor" % AKKA_VERSION
    val akkaHttp = "com.typesafe.akka" %% "akka-http-core" % AKKA_HTTP_VERSION
    val akkaHttpExp = "com.typesafe.akka" %% "akka-http" % AKKA_HTTP_VERSION
    val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % AKKA_VERSION
    val akkaRemote = "com.typesafe.akka" %% "akka-remote" % AKKA_VERSION
    val akkaSSL = "com.typesafe" %% "ssl-config-akka" % AKKA_SSL_VERSION
    val slf4j = "org.slf4j" % "slf4j-api" % SLF4J_VERSION
    val logback = "ch.qos.logback" % "logback-classic" % LOGBACK_VERSION
    val slf4jOverLog4j = "org.slf4j" % "log4j-over-slf4j" % SLF4J_VERSION
    val json4sJackson = "org.json4s" %% "json4s-jackson" % JSON4S_VERSION
    val json4sExt = "org.json4s" %% "json4s-ext" % JSON4S_VERSION
    val metricsCore = "io.dropwizard.metrics" % "metrics-core" % METRICS_VERSION
    val metricsJvm = "io.dropwizard.metrics" % "metrics-jvm" % METRICS_VERSION
    val joda = "joda-time" % "joda-time" % JODA_VERSION

    val akkaKafka = "com.sclasen" %% "akka-kafka" % "0.1.0"
    val metricsStatsd = ("com.github.jjagged" % "metrics-statsd" % "1.0.0")
      .excludeAll(ExclusionRule(organization = "com.codahale.metrics"))

    val metricsDataDog = "org.coursera" % "metrics-datadog" % "1.1.3"
  }

  object TestDep {
    val akkaTest = "com.typesafe.akka" %% "akka-testkit" % AKKA_VERSION % "test"
    val akkaHttpTest = "com.typesafe.akka" %% "akka-http-testkit" % AKKA_HTTP_VERSION % "test"
    val specsCore = "org.specs2" %% "specs2-core" % SPECS_VERSION % "test"
    val specsMock = "org.specs2" %% "specs2-mock" % SPECS_VERSION % "test"
    val scalazStream = "org.scalaz.stream" %% "scalaz-stream" % "0.7a" % "test"
  }

  import Dependencies.CompileDep._
  import Dependencies.TestDep._

  val akka = Seq(akkaActor, akkaHttp, akkaHttpExp, akkaRemote, akkaSlf4j, akkaSSL)
  val json = Seq(json4sJackson, json4sExt)
  val logging = Seq(logback, slf4j, slf4jOverLog4j)
  val metrics = Seq(metricsCore, metricsJvm)

  val base = akka ++ json ++ logging ++ metrics ++ Seq(joda, scalaConfig)
  val test = Seq(akkaTest, akkaHttpTest, specsCore, specsMock, scalazStream)

  val core = base ++ test
  val reporting = test ++ Seq(metricsStatsd, metricsDataDog)
  val examples = Seq(akkaKafka)

  val overrrides = Set(joda, metricsCore, slf4j)
}