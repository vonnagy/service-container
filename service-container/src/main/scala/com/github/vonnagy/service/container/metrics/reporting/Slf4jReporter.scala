package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

class Slf4jReporter(implicit val system: ActorSystem, val config: Config) extends ScheduledReporter {

  val reporter = com.codahale.metrics.Slf4jReporter.forRegistry(metrics.metricRegistry)
    .outputTo(LoggerFactory.getLogger(config.getString("logger")))
    .convertRatesTo(TimeUnit.SECONDS)
    .convertDurationsTo(TimeUnit.MILLISECONDS).build

  /**
   * This is the method that gets called so that the  metrics
   * reporting can occur.
   */
  def report(): Unit = {

    reporter.report(metrics.metricRegistry.getGauges(),
      metrics.metricRegistry.getCounters(),
      metrics.metricRegistry.getHistograms(),
      metrics.metricRegistry.getMeters(),
      metrics.metricRegistry.getTimers());
  }
}
