package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.coursera.metrics.datadog.transport.Transport
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike

import scala.collection.mutable
import scala.concurrent.duration.{FiniteDuration, _}

class DogStatsDReporterSpec extends TestKit(ActorSystem()) with SpecificationLike with Mockito {

  "The DatadogReporter reporter" should {

    "report metrics when triggered by the scheduler" in {

      implicit val conf = ConfigFactory.parseString(
        """
         {
          enabled=on
          reporting-interval=10ms
          metric-prefix = "pref"
          tags = ["boo", "hoo"]
          api-key = "abc123"
        }
        """)

      val rpt = spy(new DogStatsDReporter)
      val transport = mock[Transport]
      org.mockito.Mockito.when(rpt.getTransport).thenReturn(transport)

      val rptr = mock[org.coursera.metrics.datadog.DatadogReporter]
      org.mockito.Mockito.when(rpt.getReporter).thenReturn(rptr)

      rpt.start(FiniteDuration(2, TimeUnit.MILLISECONDS))
      there was after(100.millisecond).atLeastOne(rpt).report()

      rpt.tags must containAllOf(Seq("boo", "hoo", "app:container-service", "version:1.0.0.N/A"))
      rpt.prefix must be equalTo "pref"

      rpt.stop
      there was one(transport).close()
    }
  }

  step {
    system.shutdown()
    system.awaitTermination()
  }

}


