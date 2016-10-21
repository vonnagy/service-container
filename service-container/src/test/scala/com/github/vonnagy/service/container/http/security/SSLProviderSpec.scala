package com.github.vonnagy.service.container.http.security

import akka.actor.Actor
import akka.http.scaladsl.{HttpConnectionContext, HttpsConnectionContext}
import akka.testkit.TestActorRef
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import org.specs2.mutable.SpecificationLike

class SSLProviderSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  val act = TestActorRef(new SSLActor)

  class SSLActor extends Actor with SSLProvider {
    def receive = {
      case _ =>
    }
  }

  "SSLProviderSpec" should {

    "allow for getting an SSL context" in {
      val ctx = act.underlyingActor.getContext(true)
      ctx.isInstanceOf[HttpsConnectionContext] must beTrue
    }

    "allow for getting an non-SSL context" in {
      val ctx = act.underlyingActor.getContext(false)
      ctx.isInstanceOf[HttpConnectionContext] must beTrue
    }

  }
}
