import sbt._

object Dependencies {
  val AKKA_VERSION = "2.5.23"
  val AKKA_HTTP_VERSION = "10.1.8"
  val AKKA_STREAM_KAFKA = "1.0.4"
  val CONFIG_VERSION = "1.3.4"
  val JODA_VERSION = "2.10.2"
  val JSON4S_VERSION = "3.6.7"
  val LOGBACK_VERSION = "1.2.3"
  val METRICS_DATADOG = "1.1.13"
  val METRICS_STATSD = "1.0.0"
  val METRICS_VERSION = "4.1.0"
  val SLF4J_VERSION = "1.7.26"
  val SPECS_VERSION = "4.6.0"

  object CompileDep {
    val config = "com.typesafe" % "config" % CONFIG_VERSION
    val akkaActor = "com.typesafe.akka" %% "akka-actor" % AKKA_VERSION
    val akkaHttp = "com.typesafe.akka" %% "akka-http-core" % AKKA_HTTP_VERSION
    val akkaHttpExp = "com.typesafe.akka" %% "akka-http" % AKKA_HTTP_VERSION
    val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % AKKA_VERSION
    val akkaRemote = "com.typesafe.akka" %% "akka-remote" % AKKA_VERSION
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % AKKA_VERSION
    val slf4j = "org.slf4j" % "slf4j-api" % SLF4J_VERSION
    val logback = "ch.qos.logback" % "logback-classic" % LOGBACK_VERSION
    val slf4jOverLog4j = "org.slf4j" % "log4j-over-slf4j" % SLF4J_VERSION
    val json4sJackson = "org.json4s" %% "json4s-jackson" % JSON4S_VERSION
    val json4sExt = "org.json4s" %% "json4s-ext" % JSON4S_VERSION
    val metricsCore = "io.dropwizard.metrics" % "metrics-core" % METRICS_VERSION
    val metricsJvm = "io.dropwizard.metrics" % "metrics-jvm" % METRICS_VERSION
    val joda = "joda-time" % "joda-time" % JODA_VERSION

    val akkaKafka = "com.typesafe.akka" %% "akka-stream-kafka" % AKKA_STREAM_KAFKA

    val metricsStatsd = ("com.github.jjagged" % "metrics-statsd" % METRICS_STATSD)
      .exclude("com.codahale.metrics", "metrics-core")

    val metricsDataDog = "org.coursera" % "metrics-datadog" % METRICS_DATADOG
  }

  object TestDep {
    val akkaTest = "com.typesafe.akka" %% "akka-testkit" % AKKA_VERSION % "test"
    val akkaHttpTest = "com.typesafe.akka" %% "akka-http-testkit" % AKKA_HTTP_VERSION % "test"
    val akkaStreamTest = "com.typesafe.akka" %% "akka-stream-testkit" % AKKA_VERSION % "test"
    val specsCore = "org.specs2" %% "specs2-core" % SPECS_VERSION % "test"
    val specsMock = "org.specs2" %% "specs2-mock" % SPECS_VERSION % "test"
  }

  import Dependencies.CompileDep._
  import Dependencies.TestDep._

  val akka = Seq(akkaActor, akkaHttp, akkaHttpExp, akkaRemote, akkaSlf4j, akkaStream)
  val json = Seq(json4sJackson, json4sExt)
  val logging = Seq(logback, slf4j, slf4jOverLog4j)
  val metrics = Seq(metricsCore, metricsJvm)

  val base = akka ++ json ++ logging ++ metrics ++ Seq(joda)
  val test = Seq(akkaTest, akkaHttpTest, akkaStreamTest, specsCore, specsMock)

  val core = base ++ test
  val reporting = test ++ Seq(metricsStatsd, metricsDataDog)
  val examples = Seq(akkaKafka)

  val overrrides = Set(joda, metricsCore, slf4j) ++ akka
}