package com.github.vonnagy.service.container.metrics.reporting

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import com.typesafe.config.Config
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration._

/**
 * Created by Ivan von Nagy on 1/14/15.
 */
class ScheduledReporterSpec extends AkkaTestkitSpecs2Support with SpecificationLike with Mockito {

  implicit val conf = system.settings.config

  val rpt = spy(new TestReporter)

  "The scheduled reporter" should {

    "allow for creation given a specific actor system" in {
      rpt must not beNull
    }

    "provide container and application information" in {
      rpt.application must be equalTo ("Container Service")
      rpt.version must be equalTo ("1.0.0.N/A")
      rpt.host must not be equalTo("")
    }

    "allow to start and stop a scheduled a reporting interval" in {
      rpt.start(FiniteDuration(2, TimeUnit.MILLISECONDS))
      rpt.schedule.isDefined must beTrue

      there was after(100.millisecond).atLeastOne(rpt).report()

      rpt.stop
      rpt.schedule.isDefined must beFalse
    }
  }

  class TestReporter(implicit val system: ActorSystem, val config: Config) extends ScheduledReporter {
    override def report(): Unit = {
      // Do nothing
    }
  }

}
