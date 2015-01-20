package com.github.vonnagy.service.container.http

import akka.actor.ActorDSL._
import akka.testkit.{TestActorRef, TestProbe}
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import com.github.vonnagy.service.container.health.HealthState
import org.specs2.mutable.SpecificationLike
import spray.util.Utils

class HttpServiceSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  sequential
  val (hostname, httpPort) = Utils.temporaryServerHostnameAndPort()

  val probe = TestProbe()

  val act = TestActorRef(new Act with HttpService {
    val httpInterface = hostname
    val port = httpPort

    become(httpStarting orElse ({
      case HttpStarted => probe.ref ! HttpStarted
    }: Receive))

    whenStopping {
      stopHttpServer
    }
  }, "service")

  "The HttpService" should {

    "be able to check the services health before it is started" in {
      act.underlyingActor.getHttpHealth must not be null
      act.underlyingActor.getHttpHealth.state must be equalTo HealthState.CRITICAL
    }

    "be able to start and Http service on a specified port" in {
      act.underlyingActor.httpBound must beFalse
      act.underlyingActor.startHttpServer(Nil)
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
