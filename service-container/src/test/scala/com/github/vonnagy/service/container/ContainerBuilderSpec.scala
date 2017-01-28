package com.github.vonnagy.service.container

import com.github.vonnagy.service.container.health.{HealthCheck, HealthInfo, HealthState}
import com.github.vonnagy.service.container.listener.TestContainerLifecycleListener
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContainerBuilderSpec extends Specification {

  sequential

  "The container" should {

    "allow for defining endpoints" in {

      val cont = new ContainerBuilder()
        .withRoutes(classOf[TestEndpoints])
        .build


      val routes = cont.registeredRoutes
      cont.shutdown
      routes.length must be equalTo 1
    }

    "allow for defining listeners" in {

      val cont = new ContainerBuilder()
        .withListeners(new TestContainerLifecycleListener())
        .build


      val listeners = cont.listeners
      cont.shutdown
      listeners.length must be equalTo 1
    }


    "allow for defining health checks" in {

      val cont = new ContainerBuilder().withHealthChecks(new HealthCheck {
        override def getHealth: Future[HealthInfo] = Future {
          HealthInfo("test", HealthState.CRITICAL, "")
        }
      }).build

      val checks = cont.registeredHealthChecks
      cont.shutdown
      checks.length must be equalTo 1
    }

  }
}

