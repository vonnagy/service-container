package com.github.vonnagy.service.container.health

import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import org.specs2.mutable.SpecificationLike

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class HealthProviderSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  sequential
  val sys = system

  import system.dispatcher

  "The HealthProvider" should {

    "properly gather the container health" in {

      Health(system).addCheck(new HealthCheck {
        override def getHealth: Future[HealthInfo] = Future {
          HealthInfo("healthy", HealthState.OK, "details")
        }
      })

      val prov = new HealthProvider {
        implicit val system = sys
        implicit val executor = system.dispatcher
      }

      val health = Await.result[ContainerHealth](prov.runChecks, 1 second)
      health.state must be equalTo HealthState.OK
    }

    "properly gather the container health when the system is not healthy" in {

      val prov = new HealthProvider {
        implicit val system = sys
        implicit val executor = system.dispatcher
      }

      Health(system).addCheck(new HealthCheck {
        override def getHealth: Future[HealthInfo] = Future {
          HealthInfo("critical", HealthState.CRITICAL, "details", checks = List(HealthInfo("critical-child", HealthState.CRITICAL, "child")))
        }
      })

      val health = Await.result[ContainerHealth](prov.runChecks, 1 second)
      health.state must be equalTo HealthState.CRITICAL
    }
  }

}
