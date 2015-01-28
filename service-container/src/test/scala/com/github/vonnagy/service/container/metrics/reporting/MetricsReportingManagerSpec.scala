package com.github.vonnagy.service.container.metrics.reporting

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.github.vonnagy.service.container.health.{GetHealth, HealthInfo, HealthState}
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.SpecificationLike

/**
 * Created by Ivan von Nagy on 1/14/15.
 */
class MetricsReportingManagerSpec
  extends TestKit(ActorSystem("default",
    ConfigFactory.parseString("container.metrics.reporters.Slf4j.enabled=on")))
  with SpecificationLike {

  // Run in order
  sequential

  "The MetricsReportingManager" should {

    val probe = TestProbe()
    val act = TestActorRef[MetricsReportingManager](MetricsReportingManager.props())

    "be able to load the defined reporters" in {
      act.underlyingActor.reporters.size must be equalTo (1)
    }

    "be able to report it's health" in {
      probe.send(act, GetHealth)
      probe.expectMsgClass(classOf[HealthInfo]) must beEqualTo(HealthInfo("metrics-reporting",
        HealthState.OK, "The system is currently managing 1 metrics reporters",
        Some(List("com.github.vonnagy.service.container.metrics.reporting.Slf4jReporter")), List()))
    }

    "be able to stop the running reporters" in {
      act.underlyingActor.stopReporters
      act.underlyingActor.reporters.size must be equalTo (0)
    }


  }
}
