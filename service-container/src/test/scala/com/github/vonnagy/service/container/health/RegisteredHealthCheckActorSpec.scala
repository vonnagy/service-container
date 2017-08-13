package com.github.vonnagy.service.container.health

import akka.actor._
import akka.testkit.TestActorRef
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class RegisteredHealthCheckActorSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  sequential

  "Health check registration" should {

    "allow for the creation of a registered health check" in {

      val r = new TestRegisteredHealthCheck()(system)
      Await.result[HealthInfo](r.getHealth, 1 second).state must be equalTo HealthState.OK
      Health(system).getChecks.length must be equalTo 1
    }

    "allow for the creation of a registered health check actor" in {

      val ext = Health(system)
      val act = TestActorRef(new Actor with RegisteredHealthCheckActor {
        def receive = {
          case GetHealth => sender ! HealthInfo("test", HealthState.OK, "details")
        }
      })

      Await.result[HealthInfo](act.underlyingActor.getHealth, 1 second).state must be equalTo HealthState.OK
      ext.getChecks.length must be equalTo 2
    }
  }
}

class TestRegisteredHealthCheck(implicit val system: ActorSystem) extends RegisteredHealthCheck {

  import system.dispatcher

  def getHealth: Future[HealthInfo] = Future {
    HealthInfo("test", HealthState.OK, "details")
  }
}


