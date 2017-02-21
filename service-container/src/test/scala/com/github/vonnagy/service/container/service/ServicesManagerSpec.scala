package com.github.vonnagy.service.container.service

import akka.actor.{Actor, ActorNotFound, ActorRef, ActorSystem, Props, Terminated}
import akka.testkit.{TestActorRef, TestProbe}
import akka.util.Timeout
import com.github.vonnagy.service.container.health.{GetHealth, HealthInfo, HealthState}
import com.github.vonnagy.service.container.http.HttpStopped
import com.github.vonnagy.service.container.service.ServicesManager.{FindService, ServiceNotFound, ShutdownService, StatusRunning}
import com.github.vonnagy.service.container.{AkkaTestkitSpecs2Support, TestUtils}
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Failure

class ServicesManagerSpec extends AkkaTestkitSpecs2Support(ActorSystem("test", {
  val http = TestUtils.temporaryServerHostnameAndPort()
  ConfigFactory.parseString(
    s"""
        akka.log-dead-letters-during-shutdown=off
        container.http.interface="${http._2}"
        container.http.port=${http._3}
        """).withFallback(ConfigFactory.load())
})) with SpecificationLike with Mockito {

  sequential

  val probe = TestProbe()
  val containerService = mock[ContainerService]
  lazy val act = TestActorRef[ServicesManager](ServicesManager.props(containerService, Nil, Nil), "service")

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

      // Make sure we are running
      probe.send(act, StatusRunning)
      probe.expectMsg(5 seconds, true) must beTrue

      // Hijack all of the messages
      val intercept: PartialFunction[Any, Any] = {
        case m@HttpStopped =>
          act.underlyingActor.log.info(s"Intercepted HttpStopped message")
          probe.ref ! m
          m
        case msg => msg
      }

      // Switch out the handlers with our interceptor
      act.underlying.become(intercept andThen act.underlyingActor.running, true)

      // Stop the Http actor
      act.underlying.children.foreach(child => system.stop(child))

      probe.expectMsg(5 seconds, HttpStopped)

      probe.send(act, GetHealth)
      val msg = probe.expectMsgType[HealthInfo]
      msg.state must be equalTo (HealthState.CRITICAL)
    }

    "be able to send a Shutdown message to the service" in {

      // Make sure we have the proper handler
      act.underlying.become(act.underlyingActor.running)
      probe.send(act, ShutdownService(false))
      act.underlyingActor.shutDownAndExit must beEqualTo(Some(false)).eventually(3, 20 milliseconds)
      there was after(100 milliseconds).one(containerService).shutdown
    }

    "be able to shutdown the actor properly" in {
      probe.watch(act)
      act.stop
      probe.expectMsgClass(classOf[Terminated]) must not beNull
    }

    "be able to send the actor a shutdown message and have it terminate the entire system" in {
      val cont = new ContainerService(Nil, Nil, Nil)
      val act = TestActorRef[ServicesManager](ServicesManager.props(cont, Nil, Nil), "service2")

      probe.send(act, ShutdownService)
      cont.started must beFalse.eventually(3, 500 milliseconds)
    }

    "be able to find a registered service by name" in {
      val props = Seq("test_service" -> Props[TestService])
      val act = TestActorRef[ServicesManager](ServicesManager.props(containerService, Nil, props), "service3")
      act.underlying.become(act.underlyingActor.running)
      probe.send(act, FindService("test_service"))
      val msg = probe.expectMsgType[Option[ActorRef]]
      msg.get.path.name must be equalTo ("test_service")
      val serviceProbe = TestProbe()
      serviceProbe.send(msg.get, "Hello")
      val smsg = serviceProbe.expectMsgType[String]
      smsg must be equalTo ("Hello")
    }

    "be able to find a registered service by name using the companion object" in {
      import akka.pattern.ask
      import system.dispatcher
      implicit val timeout = Timeout(1 second)
      val props = Seq("test_service_cp" -> Props[TestService])
      val act = TestActorRef[ServicesManager](ServicesManager.props(containerService, Nil, props), "service4")
      act.underlying.become(act.underlyingActor.running)
      val service = ServicesManager.findService("test_service_cp", "test/user/service4")
      Await.result(service.map(_.path.name), 1 second) must be_==("test_service_cp")
      Await.result(service.flatMap(_ ? "Hello World"), 1 second) must be_==("Hello World")
    }

    "return a failure when using the wrong service manager path" in {
      implicit val timeout = Timeout(1 second)
      val act = TestActorRef[ServicesManager](ServicesManager.props(containerService, Nil, Nil), "service5")
      act.underlying.become(act.underlyingActor.running)
      val service = ServicesManager.findService("test_cp", "test/user/service_wrong")
      service.value.get must beAnInstanceOf[Failure[ActorNotFound]].eventually
    }

    "return a failure when a service can't be found" in {
      implicit val timeout = Timeout(1 second)
      val props = Seq("test_service_cp" -> Props[TestService])
      val act = TestActorRef[ServicesManager](ServicesManager.props(containerService, Nil, props), "service6")
      act.underlying.become(act.underlyingActor.running)
      val service = ServicesManager.findService("unknown_service", "test/user/service6")
      service.value.get must beAnInstanceOf[Failure[ServiceNotFound]].eventually
    }
  }
}

private[this] class TestService extends Actor {
  override def receive: Receive = {
    case any =>
      sender ! any
  }
}
