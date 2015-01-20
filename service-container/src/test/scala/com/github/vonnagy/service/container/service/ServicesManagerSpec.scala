package com.github.vonnagy.service.container.service

import akka.actor.{ActorSystem, Terminated}
import akka.testkit.{TestActorRef, TestProbe}
import com.github.vonnagy.service.container.health.{GetHealth, HealthInfo, HealthState}
import com.github.vonnagy.service.container.http.HttpStopped
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import spray.util.Utils

class ServicesManagerSpec extends Specification {

  sequential

  val (hostname, httpPort) = Utils.temporaryServerHostnameAndPort()

  implicit val system = ActorSystem.create("test",
    ConfigFactory.parseString( s"""
      akka.log-dead-letters-during-shutdown=off
      container.http.interface="${hostname}"
      container.http.port=${httpPort}
      """).withFallback(ConfigFactory.load()))

  val probe = TestProbe()
  val act = TestActorRef[ServicesManager](ServicesManager.props(Nil, Nil), "service")

  "The ServicesManager" should {

    "be able to start an Http service on a specified port" in {
      probe.send(act, StatusRunning)
      probe.expectMsg(true) must beTrue
    }

    "be able to fetch the system's health" in {
      probe.send(act, GetHealth)
      probe.expectMsgClass(classOf[HealthInfo]) must not beNull
    }

    "be able to receive an HttpStopped message when stopped" in {

      // Hijack all of the messages
      val intercept: PartialFunction[Any, Any] = {
        case (m @ HttpStopped) => probe.ref ! m; m
        case msg => msg
      }
      act.underlying.become(intercept andThen act.underlyingActor.running, false)

      act.underlyingActor.stopHttpServer
      probe.expectMsg(HttpStopped)
      probe.send(act, GetHealth)

      val msg = probe.expectMsgType[HealthInfo]
      msg.state must be equalTo (HealthState.CRITICAL)
    }

    "be able to shutdown properly" in {
      probe.watch(act);
      act.stop
      probe.expectMsgClass(classOf[Terminated]) must not beNull
    }
  }

  step {
    system.shutdown
    system.awaitTermination
  }

}
