package com.github.vonnagy.service.container.health

import akka.actor.ActorSystem
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import org.specs2.mutable.SpecificationLike

import scala.concurrent.Future

class HealthCheckExtensionSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  "The health check extension" should {

    "allow for creation given a specific actor system" in {
      val ext = Health(system)
      ext must not be null
    }

    "allow for creation given a implicit actor system" in {
      val ext = Health()
      ext must not be null
    }

    "allow for addition and fetch of checks" in {
      val ext = Health(system)
      ext must not be null
      ext.addCheck(new HealthCheck {
        override def getHealth: Future[HealthInfo] = Future {
          HealthInfo("test", HealthState.OK, "details")
        }
      })
      ext.getChecks.length must be equalTo 1
    }

    "throw an exception when no ActorSystem is in scope" in {
      implicit val system: ActorSystem = null
      Health() must throwA[Exception]
    }

  }

}
