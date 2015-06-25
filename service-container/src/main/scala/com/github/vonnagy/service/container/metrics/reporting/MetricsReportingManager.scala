package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import akka.ConfigurationException
import akka.actor.{Actor, ActorSystem, Props}
import com.github.vonnagy.service.container.health.{GetHealth, HealthInfo, HealthState, RegisteredHealthCheckActor}
import com.github.vonnagy.service.container.log.ActorLoggingAdapter
import com.github.vonnagy.service.container.metrics.Metrics
import com.typesafe.config.{ConfigRenderOptions, Config}

import scala.collection.convert.WrapAsScala
import scala.concurrent.duration.FiniteDuration

object MetricsReportingManager {
  def props(): Props =
    Props(classOf[MetricsReportingManager])
}

/**
 * This is the reporting manage for metrics. It is responsible for launching all of the defined
 * reporters for the system.
 */
class MetricsReportingManager extends Actor with RegisteredHealthCheckActor with ActorLoggingAdapter {

  // Get the metrics extension
  private val metrics = Metrics(context.system)

  private[reporting] var reporters = Seq.empty[ScheduledReporter]

  def receive = {
    case GetHealth => sender ! checkHealth
  }

  override def preStart(): Unit = {
    // Start the defined reporters
    startReporters
  }


  override def postStop(): Unit = {
    // Stop the running reporters
    stopReporters
  }

  /**
   * Load the defined reporters
   */
  private[reporting] def startReporters(): Unit = {
    try {
      val master = context.system.settings.config.getConfig("container.metrics.reporters");

      val definedReporters =
        for {
          entry <- WrapAsScala.iterableAsScalaIterable(master.root.entrySet)
          if (master.getConfig(entry.getKey).getBoolean("enabled"))
        } yield {
          val config = master.getConfig(entry.getKey)
          val json = config.root.render(ConfigRenderOptions.defaults)

          val clazz = config.getString("class")

          metrics.system.dynamicAccess.createInstanceFor[ScheduledReporter](clazz,
            List(classOf[ActorSystem] -> context.system, (classOf[Config] -> config))).map({
            case reporter =>
              reporter.start(FiniteDuration(config.getDuration("reporting-interval", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))
              reporter
          }).recover({
            case e => throw new ConfigurationException(
              "Metrics reporter specified in config can't be loaded [" + clazz +
                "] due to [" + e.toString + "]", e)
          }).get
        }

      reporters = definedReporters.toSeq

    } catch {
      case e: Exception ⇒
        System.err.println("Error while starting up metric reporters")
        e.printStackTrace()
        throw new ConfigurationException("Could not start reporters due to [" + e.toString + "]")
    }
  }

  /**
   * Stop the running reporters
   */
  private[reporting] def stopReporters(): Unit = {
    reporters.foreach(_.stop)
    reporters = Seq.empty[ScheduledReporter]
  }

  private def checkHealth(): HealthInfo = {
    if (reporters.length == 0) {
      HealthInfo("metrics-reporting", HealthState.OK, s"The system is currently not managing any metrics reporters")
    }
    else {
      val x = for {
        reporter <- reporters
      } yield {
        reporter.getClass.getName
      }

      HealthInfo("metrics-reporting", HealthState.OK, s"The system is currently managing ${reporters.length} metrics reporters", Some(x))
    }
  }
}
