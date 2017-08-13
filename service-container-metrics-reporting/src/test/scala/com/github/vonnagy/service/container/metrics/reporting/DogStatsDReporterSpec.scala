package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import com.typesafe.config.ConfigFactory
import org.coursera.metrics.datadog.transport.Transport
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration.{FiniteDuration, _}

class DogStatsDReporterSpec extends AkkaTestkitSpecs2Support with SpecificationLike with Mockito {

  "The DatadogReporter reporter" should {

    "report metrics when triggered by the scheduler" in {

      implicit val conf = ConfigFactory.parseString(
        """
         {
          enabled=on
          host="localhost"
          port=8125
          reporting-interval=10ms
          metric-prefix = "pref"
          tags = ["boo", "hoo"]
          api-key = "abc123"
        }
        """)

      val dogStatsDReporter = spy(new DogStatsDReporter)

      val transport = mock[Transport]
      dogStatsDReporter.getTransport returns transport

      val rptr = mock[org.coursera.metrics.datadog.DatadogReporter]
      dogStatsDReporter.getReporter returns rptr

      dogStatsDReporter.start(FiniteDuration(2, TimeUnit.MILLISECONDS))
      there was after(100.millisecond).atLeastOne(dogStatsDReporter).report()

      dogStatsDReporter.tags must containAllOf(Seq("boo", "hoo", "app:container-service", "version:1.0.0.N/A"))
      dogStatsDReporter.prefix must be equalTo "pref"

      dogStatsDReporter.stop
      there was one(transport).close()
    }
  }

}


