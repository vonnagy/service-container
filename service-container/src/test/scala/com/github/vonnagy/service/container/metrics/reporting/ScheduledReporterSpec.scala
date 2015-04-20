package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import com.typesafe.config.{Config, ConfigFactory}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration._

/**
 * Created by Ivan von Nagy on 1/14/15.
 */
class ScheduledReporterSpec extends AkkaTestkitSpecs2Support with SpecificationLike with Mockito {

  implicit val conf = ConfigFactory.parseString(
    """
    {
      enabled=on
      reporting-interval=10ms
    }
    """)

  "The scheduled reporter" should {

    "allow for creation given a specific actor system" in {
      val rpt = spy(new TestReporter)
      rpt must not beNull
    }

    "provide container and application information" in {
      val rpt = spy(new TestReporter)
      rpt.application must be equalTo ("Container Service")
      rpt.version must be equalTo ("1.0.0.N/A")
      rpt.host must not be equalTo("")
    }

    "allow to start and stop a scheduled a reporting interval" in {
      val rpt = spy(new TestReporter)
      rpt.start(FiniteDuration(conf.getDuration("reporting-interval", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))
      rpt.schedule.isDefined must beTrue

      there was after(30.millisecond).atLeastOne(rpt).report()

      rpt.stop
      rpt.schedule.isDefined must beFalse
    }
  }

  class TestReporter(implicit val system: ActorSystem, val config: Config) extends ScheduledReporter {
    def report(): Unit = {
      // Do nothing
    }
  }

}
