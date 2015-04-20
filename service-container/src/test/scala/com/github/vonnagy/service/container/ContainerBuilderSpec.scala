package com.github.vonnagy.service.container

import com.github.vonnagy.service.container.health.{HealthCheck, HealthInfo, HealthState}
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

      cont.registeredRoutes.length must be equalTo 1
    }

    "allow for defining health checks" in {

      val cont = new ContainerBuilder().withHealthChecks(new HealthCheck {
        override def getHealth: Future[HealthInfo] = Future {
          HealthInfo("test", HealthState.CRITICAL, "")
        }
      }).build

      cont.registeredHealthChecks.length must be equalTo 1
    }

  }
}

