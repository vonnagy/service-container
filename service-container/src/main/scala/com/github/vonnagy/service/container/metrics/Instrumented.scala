package com.github.vonnagy.service.container.metrics

import akka.actor.ActorSystem

trait Instrumented {

  implicit val system: ActorSystem

  /**
   * The MetricRegistry where created metrics are registered.
   */
  val metricRegistry = Metrics().metricRegistry
}