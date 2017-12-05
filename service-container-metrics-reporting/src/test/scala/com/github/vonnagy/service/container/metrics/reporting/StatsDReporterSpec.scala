package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import com.github.jjagged.metrics.reporting.statsd.StatsD
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration._

/**
 * Created by Ivan von Nagy on 1/21/15.
 */
class StatsDReporterSpec extends AkkaTestkitSpecs2Support with SpecificationLike with Mockito {

  "The StatsDReporter reporter" should {

    "report metrics when triggered by the scheduler" in {

      implicit val conf = ConfigFactory.parseString(
        """
         {
          enabled=on
          reporting-interval=10ms
          host="localhost"
          port=9092
          metric-prefix = "pref"
        }
        """)

      val statsdReporter = spy(new StatsDReporter)
      val statsD = mock[StatsD]
      statsdReporter.getStatsD returns statsD

      val rptr = mock[com.github.jjagged.metrics.reporting.StatsDReporter]
      statsdReporter.getReporter returns rptr

      statsdReporter.start(FiniteDuration(2, TimeUnit.MILLISECONDS))
      there was after(100.millisecond).atLeastOne(statsdReporter).report()

      statsdReporter.stop
      there was one(statsD).close()
    }
  }

}

