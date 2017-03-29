package com.github.vonnagy.service.container.http

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestActorRef, TestProbe}
import com.github.vonnagy.service.container.{AkkaTestkitSpecs2Support, TestUtils}
import com.github.vonnagy.service.container.health.HealthState
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.SpecificationLike

class HttpServiceSpec extends AkkaTestkitSpecs2Support(ActorSystem("test", {
  val http = TestUtils.temporaryServerHostnameAndPort()
  val https = TestUtils.temporaryServerHostnameAndPort()

  ConfigFactory.parseString(
    s"""
      container.http.interface="${http._2}"
      container.http.port=${http._3}
      container.https.interface="${https._2}"
      container.https.port=${https._3}
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
      act.underlyingActor.httpSettings.isEmpty must beFalse
      act.underlyingActor.httpServer.isEmpty must beTrue
      probe.send(act, HttpStart)
      val msg = probe.expectMsg(HttpStarted)
      msg must be equalTo HttpStarted

      act.underlyingActor.httpServer.size must be equalTo(2)
    }

    "be able to check the services health after it is started" in {
      act.underlyingActor.getHttpHealth must not be null
      act.underlyingActor.getHttpHealth.state must be equalTo HealthState.OK
    }

    "be able to stop the Http service" in  {
      act.underlyingActor.stopHttpServer
      val msg = probe.expectMsg(HttpStopped)
      msg must be equalTo HttpStopped
      act.underlyingActor.httpServer.isEmpty must beTrue
    }
  }

}
