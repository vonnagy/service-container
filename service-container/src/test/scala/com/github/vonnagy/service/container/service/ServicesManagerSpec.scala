package com.github.vonnagy.service.container.service

import akka.actor.{ActorSystem, Terminated}
import akka.testkit.{TestActorRef, TestProbe}
import com.github.vonnagy.service.container.TestUtils
import com.github.vonnagy.service.container.health.{GetHealth, HealthInfo, HealthState}
import com.github.vonnagy.service.container.http.HttpStopped
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll

import scala.concurrent.duration._

class ServicesManagerSpec extends Specification with AfterAll {

  sequential

  val (address, hostname, httpPort) = TestUtils.temporaryServerHostnameAndPort()

  implicit val system = ActorSystem.create("test",
    ConfigFactory.parseString( s"""
      akka.log-dead-letters-during-shutdown=off
      container.http.interface="${hostname}"
      container.http.port=${httpPort}
      """).withFallback(ConfigFactory.load()))

  val probe = TestProbe()

  lazy val act = TestActorRef[ServicesManager](ServicesManager.props(null, Nil, Nil), "service")

  "The ServicesManager" should {

    "be able to get a degraded health state before the Http service is running" in {
      probe.send(act, GetHealth)
      val msg = probe.expectMsgType[HealthInfo]
      msg.state must be equalTo (HealthState.DEGRADED)
    }

    "be able to start an Http service on a specified port" in {
      probe.send(act, StatusRunning)
      probe.expectMsg(5 seconds, true) must beTrue
    }

    "be able to fetch the system's health" in {
      probe.send(act, GetHealth)
      probe.expectMsgClass(classOf[HealthInfo]) must not beNull
    }

    "be able to receive an HttpStopped message when stopped" in {

      // Hijack all of the messages
      val intercept: PartialFunction[Any, Any] = {
        case m @ HttpStopped => probe.ref ! m; m
        case msg => msg
      }
      act.underlying.become(intercept andThen act.underlyingActor.running, false)

      // Stop the Http actor
      act.underlying.actor.context.stop(act.underlying.child("http").get)

      probe.expectMsg(10 seconds, HttpStopped)
      probe.send(act, GetHealth)

      val msg = probe.expectMsgType[HealthInfo]
      msg.state must be equalTo (HealthState.CRITICAL)
    }

    "be able to shutdown the actor properly" in {
      probe.watch(act);
      act.stop
      probe.expectMsgClass(classOf[Terminated]) must not beNull
    }

    "be able to send the actor a shutdown message and have it terminate the entire system" in {
      val cont = new ContainerService(Nil, Nil, Nil)
      val act = TestActorRef[ServicesManager](ServicesManager.props(cont, Nil, Nil), "service")

      probe.send(act, ShutdownService)
      cont.started must beFalse.eventually(3, 500 milliseconds)
    }
  }

  def afterAll = {
    system.terminate().wait()
  }

}
