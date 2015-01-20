package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration.FiniteDuration

/**
 * Created by Ivan von Nagy on 1/14/15.
 */
class ScheduledReporterSpec extends AkkaTestkitSpecs2Support with SpecificationLike with Mockito {

  "The scheduled reporter" should {

    "allow for creation given a specific actor system" in {
      implicit val config = system.settings.config.getConfig("container.metrics.reporters.Slf4j")
      val rpt = new Slf4jReporter
      rpt must not beNull

    }

    "allow to start and stop a scheduled a reporting interval" in {
      implicit val config = ConfigFactory.parseString(
        """
         {
          enabled=on
          reporting-interval=30ms
          logger = "com.github.vonnagy.service.container.metrics"
        }
        """)

      val rpt = spy(new Slf4jReporter)
      rpt must not beNull

      rpt.start(FiniteDuration(config.getDuration("reporting-interval", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))
      rpt.schedule.isDefined must beTrue

      there was after(50.millisecond).one(rpt).report()

      rpt.stop
      rpt.schedule.isDefined must beFalse

    }
  }

}
