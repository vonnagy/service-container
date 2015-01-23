package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.github.jjagged.metrics.reporting.statsd.StatsD
import com.github.vonnagy.service.container.log.LoggingAdapter
import com.typesafe.config.Config

/**
 * Created by Ivan von Nagy on 1/21/15.
 */
class StatsDReporter(implicit val system: ActorSystem, val config: Config) extends ScheduledReporter with LoggingAdapter {

  private lazy val reporter = getReporter
  private lazy val statsD = getStatsD

  private val statsdHost = config.getString("host")
  private val port = config.getInt("port")
  private val prefix = config.getString("metric-prefix")

  /**
   * Stop the scheduled metric reporting
   */
  override def stop: Unit = {
    super.stop
    if (statsD != null) {
      statsD.close()
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

  private[reporting] def getReporter: com.github.jjagged.metrics.reporting.StatsDReporter = {

    log.info("Initializing the StatsD metrics reporter");

    com.github.jjagged.metrics.reporting.StatsDReporter.forRegistry(metrics.metricRegistry)
      .prefixedWith(this.prefix)
      .withTags("{'host':'" + host + "', 'application':'" + application.replace(' ', '-').toLowerCase() + "'}")
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .convertRatesTo(TimeUnit.SECONDS)
      .build(statsD);

  }

  private[reporting] def getStatsD: StatsD = {
    new StatsD(statsdHost, port);
  }
}
