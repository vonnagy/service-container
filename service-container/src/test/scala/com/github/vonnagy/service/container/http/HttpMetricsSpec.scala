package com.github.vonnagy.service.container.http

import java.util.concurrent.TimeUnit

import akka.actor.ActorDSL._
import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActorRef
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import com.github.vonnagy.service.container.metrics.Metrics
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration._

class HttpMetricsSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  sequential

  val svcAct = TestActorRef(new Act {
    become {
      case _ =>
    }

    val listener = context.actorOf(Props(
      new Act {
        become {
          case _ => sender ! Stats(FiniteDuration(1000, TimeUnit.MILLISECONDS), 1000, 1000, 1000, 1000, 1000, 1000, 1000)
        }

      }), "listener-0")
  }, "http")

  class MetricTest(implicit val system: ActorSystem) extends HttpMetrics {
    def httpListener = Some(system.actorSelection(svcAct.children.head.path))
  }

  val metrics = new MetricTest

  "The HttpMetrics" should {

    "provide basic stats" in {

      metrics.lastStats must be equalTo Stats(FiniteDuration(0, TimeUnit.MILLISECONDS), 0, 0, 0, 0, 0, 0, 0)
      metrics.totConn.name must be equalTo "container.http.connections.total"
      metrics.openConn.name must be equalTo "container.http.connections.open"
      metrics.maxOpenConn.name must be equalTo "container.http.connections.max-open"
      metrics.totReq.name must be equalTo "container.http.requests.total"
      metrics.openReq.name must be equalTo "container.http.requests.open"
      metrics.maxOpenReq.name must be equalTo "container.http.requests.max-open"
      metrics.uptime.name must be equalTo "container.http.uptime"
      metrics.idle.name must be equalTo "container.http.idle-timeouts"

      val metricRegistry = Metrics().metricRegistry
      import scala.collection.convert.WrapAsScala.mapAsScalaMap
      metricRegistry.getGauges.foreach(_._2.getValue)
      metricRegistry.getGauges().filterKeys(g => !g.startsWith("jvm.")).size must be equalTo 8
    }

    "schedule and cancel the metrics job" in {
      metrics.metricsJob must beNone
      metrics.scheduleHttpMetrics(FiniteDuration(100, TimeUnit.MILLISECONDS))
      metrics.metricsJob must not beNone

      metrics.cancelHttpMetrics
      metrics.metricsJob must beNone
    }

    "schedule and fetch the metrics" in {
      metrics.metricsJob must beNone
      metrics.scheduleHttpMetrics(FiniteDuration(100, TimeUnit.MILLISECONDS))
      Thread.sleep(1000)

      metrics.cancelHttpMetrics
      metrics.lastStats must not be Stats(FiniteDuration(0, TimeUnit.MILLISECONDS), 0, 0, 0, 0, 0, 0, 0)

    }

  }
}
