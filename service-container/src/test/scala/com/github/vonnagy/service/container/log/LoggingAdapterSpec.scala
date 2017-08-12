package com.github.vonnagy.service.container.log

import akka.actor._
import akka.testkit.{TestActorRef, TestProbe}
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import org.specs2.mutable.SpecificationLike

/**
 * Created by Ivan von Nagy on 1/15/15.
 */
class LoggingAdapterSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  "The LoggingAdapter" should {

    "allow an actor to use the logger" in {

      val act = TestActorRef(new Actor with ActorLoggingAdapter {
        def receive = {
          case _ => log.info("got it"); sender ! "got it"
        }
      }, "logger-test")

      // Make sure everything is all setup
      act.underlyingActor.logSrc must not beNull

      act.underlyingActor.log must not beNull

      act.underlyingActor.log.getName must beEqualTo("akka.testkit.TestActorRef")

      // Send a message and make sure we get a response back
      val probe = TestProbe()
      probe.send(act, "test")
      probe.expectMsgType[String] must beEqualTo("got it")
    }
  }
}
