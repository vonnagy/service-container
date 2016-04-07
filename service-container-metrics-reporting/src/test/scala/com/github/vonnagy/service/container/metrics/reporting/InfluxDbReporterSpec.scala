package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import metrics_influxdb.{Influxdb, InfluxdbReporter}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration._

/**
  * Created by Ivan von Nagy on 1/21/15.
  */
class InfluxDbReporterSpec extends AkkaTestkitSpecs2Support with SpecificationLike with Mockito {

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

}


