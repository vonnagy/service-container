package com.github.vonnagy.service.container.metrics

import akka.actor.ActorSystem
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import org.specs2.mutable.SpecificationLike

class MetricsExtensionSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  "The metrics extension" should {

    "allow for creation given a specific actor system" in {
      val ext = Metrics(system)
      ext must not be null
    }

    "allow for creation given a implicit actor system" in {
      val ext = Metrics()
      ext must not be null
    }

    "create the registry and access a metric" in {
      val ext = Metrics(system)
      ext must not be null
      val reg = ext.metricRegistry
      reg.counter("test")
      reg should not be null
      reg.getCounters.containsKey("test") must be equalTo true

    }

    "allow for Java access to the extension" in {
      val ext = Metrics.get(system)
      ext must not be null
    }

    "throw an exception when no ActorSystem is in scope" in {
      implicit val system: ActorSystem = null
      Metrics() must throwA[Exception]
    }


  }
}

