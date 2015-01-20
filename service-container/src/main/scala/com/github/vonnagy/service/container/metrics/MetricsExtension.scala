package com.github.vonnagy.service.container.metrics

import java.lang.management.ManagementFactory

import akka.actor._
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.jvm.{BufferPoolMetricSet, GarbageCollectorMetricSet, MemoryUsageGaugeSet, ThreadStatesGaugeSet}

class MetricsExtension(extendedSystem: ExtendedActorSystem) extends Extension {

  // Allow access to the extended system
  val system = extendedSystem
  // The application wide metrics registry.
  val metricRegistry = new MetricRegistry()

  // Register the Jvm metrics
  val srv = ManagementFactory.getPlatformMBeanServer
  metricRegistry.register("jvm.buffer-pool", new BufferPoolMetricSet(srv))
  metricRegistry.register("jvm.gc", new GarbageCollectorMetricSet)
  metricRegistry.register("jvm.memory", new MemoryUsageGaugeSet)
  metricRegistry.register("jvm.thread", new ThreadStatesGaugeSet)
}

object Metrics extends ExtensionId[MetricsExtension]
with ExtensionIdProvider {

  //The lookup method is required by ExtensionIdProvider,
  // so we return ourselves here, this allows us
  // to configure our extension to be loaded when
  // the ActorSystem starts up
  override def lookup = Metrics

  //This method will be called by Akka
  // to instantiate our Extension
  override def createExtension(system: ExtendedActorSystem) = new MetricsExtension(system)

  def apply()(implicit system: ActorSystem): MetricsExtension = system.registerExtension(this)

}