package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration._

/**
 * Created by Ivan von Nagy on 1/14/15.
 */
class Slf4jReporterSpec extends AkkaTestkitSpecs2Support with SpecificationLike with Mockito {

  "The Slf4j reporter" should {

    "report metrics when triggered by the scheduler" in {
      implicit val config = system.settings.config.getConfig("container.metrics.reporters.Slf4j")

      val rpt = spy(new Slf4jReporter)

      val rptr = mock[com.codahale.metrics.Slf4jReporter]
      org.mockito.Mockito.when(rpt.getReporter).thenReturn(rptr)

      rpt.start(FiniteDuration(2, TimeUnit.MILLISECONDS))
      there was after(30.millis).atLeastOne(rpt).report()
    }
  }

}
