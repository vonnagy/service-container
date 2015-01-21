package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.TestKit
import metrics_influxdb.{InfluxdbReporter, Influxdb}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration.FiniteDuration

/**
 * Created by Ivan von Nagy on 1/21/15.
 */
class InfluxDbReporterSpec extends TestKit(ActorSystem()) with SpecificationLike with Mockito {

  "The InfluxDbReporter reporter" should {

    "report metrics when triggered by the scheduler" in {

      implicit val conf = system.settings.config.getConfig("container.metrics.reporters.influx")

      val rpt = spy(new InfluxDbReporter)
      val remote = mock[Influxdb]
      org.mockito.Mockito.when(rpt.getInfluxDb).thenReturn(remote)

      val rptr = mock[InfluxdbReporter]
      org.mockito.Mockito.when(rpt.getReporter).thenReturn(rptr)

      rpt.start(FiniteDuration(2, TimeUnit.MILLISECONDS))
      there was after(30.millisecond).atLeastOne(rpt).report()
    }
  }

  step {
    system.shutdown()
    system.awaitTermination()
  }

}


