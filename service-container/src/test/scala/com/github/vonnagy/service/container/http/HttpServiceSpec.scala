package com.github.vonnagy.service.container.http

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestActorRef, TestProbe}
import com.github.vonnagy.service.container.{AkkaTestkitSpecs2Support, TestUtils}
import com.github.vonnagy.service.container.health.HealthState
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.SpecificationLike

class HttpServiceSpec extends AkkaTestkitSpecs2Support(ActorSystem("test", {
  val (a, h, p) = TestUtils.temporaryServerHostnameAndPort()
  ConfigFactory.parseString(
    s"""
      container.http.interface="${h}"
      container.http.port=${p}
    """)})) with SpecificationLike {

  sequential
  val probe = TestProbe()
  val act = TestActorRef[HttpService](Props(new HttpService(Nil)), probe.testActor, "service")

  "The HttpService" should {

    "be able to check the services health before it is started" in {
      act.underlyingActor.getHttpHealth must not be null
      act.underlyingActor.getHttpHealth.state must be equalTo HealthState.CRITICAL
    }

    "be able to start and Http service on a specified port" in {
      act.underlyingActor.httpServer.isDefined must beFalse
      probe.send(act, HttpStart)
      val msg = probe.expectMsg(HttpStarted)
      msg must be equalTo HttpStarted
      success
    }

    "be able to check the services health after it is started" in {
      act.underlyingActor.getHttpHealth must not be null
      act.underlyingActor.getHttpHealth.state must be equalTo HealthState.OK
    }
  }

}
