package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.github.jjagged.metrics.reporting.statsd.StatsD
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration.FiniteDuration

/**
 * Created by Ivan von Nagy on 1/21/15.
 */
class StatsDReporterSpec extends TestKit(ActorSystem()) with SpecificationLike with Mockito {

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

      val rpt = spy(new StatsDReporter)
      val statsD = mock[StatsD]
      org.mockito.Mockito.when(rpt.getStatsD).thenReturn(statsD)

      val rptr = mock[com.github.jjagged.metrics.reporting.StatsDReporter]
      org.mockito.Mockito.when(rpt.getReporter).thenReturn(rptr)

      rpt.start(FiniteDuration(2, TimeUnit.MILLISECONDS))
      there was after(30.millisecond).atLeastOne(rpt).report()

      rpt.stop
      there was one(statsD).close()
    }
  }

  step {
    system.shutdown()
    system.awaitTermination()
  }

}

