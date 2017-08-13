package com.github.vonnagy.service.container.metrics.reporting

import akka.actor.{ActorSystem, Cancellable}
import com.github.vonnagy.service.container.health.ContainerInfo
import com.github.vonnagy.service.container.metrics.Metrics
import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration

class A()(implicit val system: ActorSystem, val config: Config) extends ScheduledReporter {
  def report() = {}
}

class B()(implicit val system: ActorSystem, val config: Config) extends ScheduledReporter {
  def report() = {}
}

abstract class ScheduledReporter {

  implicit val system: ActorSystem
  implicit val config: Config

  val metrics = Metrics(system)
  val host = ContainerInfo.host
  val application = ContainerInfo.application
  val version = ContainerInfo.applicationVersion

  private[reporting] var schedule: Option[Cancellable] = None

  /**
   * Start the metrics reporting. This will delay that first report by the
   * given interval and then will continually run at the same interval.
   * @param interval
   */
  def start(interval: FiniteDuration): Unit = {
    import system.dispatcher
    schedule = Some(system.scheduler.schedule(interval, interval)(report()))

  }

  /**
   * Stop the scheduled metric reporting
   */
  def stop(): Unit = {
    schedule.exists(_.cancel())
    schedule = None
  }

  /**
   * This is the method that gets called so that the  metrics
   * reporting can occur.
   */
  def report(): Unit

}
