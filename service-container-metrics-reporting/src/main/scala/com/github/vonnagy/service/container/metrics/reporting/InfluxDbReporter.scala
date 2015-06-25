package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.github.vonnagy.service.container.log.LoggingAdapter
import com.typesafe.config.Config
import metrics_influxdb.{Influxdb, InfluxdbReporter}

/**
 * Created by Ivan von Nagy on 1/21/15.
 */
class InfluxDbReporter(implicit val system: ActorSystem, val config: Config) extends ScheduledReporter with LoggingAdapter {

  private lazy val reporter = getReporter
  private lazy val influxDb = getInfluxDb

  private val influxdbHost = config.getString("host")
  private val port = config.getInt("port")
  private val database = config.getString("database")
  private val user = config.getString("user")
  private val password = config.getString("password")
  private val prefix = config.getString("metric-prefix")

  /**
   * Stop the scheduled metric reporting
   */
  override def stop(): Unit = {
    super.stop
    if (influxDb != null) {
      influxDb
    }
  }

  /**
   * This is the method that gets called so that the  metrics
   * reporting can occur.
   */
  def report(): Unit = {

    reporter.report(metrics.metricRegistry.getGauges(),
      metrics.metricRegistry.getCounters(),
      metrics.metricRegistry.getHistograms(),
      metrics.metricRegistry.getMeters(),
      metrics.metricRegistry.getTimers())
  }

  private[reporting] def getReporter(): metrics_influxdb.InfluxdbReporter = {

    log.info("Initializing the InfluxDb metrics reporter");
    InfluxdbReporter
      .forRegistry(metrics.metricRegistry)
      .prefixedWith(prefix)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build(influxDb)
  }

  private[reporting] def getInfluxDb(): Influxdb = {
    new Influxdb(influxdbHost, port, database, user, password, TimeUnit.MILLISECONDS)
  }

}
