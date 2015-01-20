package com.github.vonnagy.service.container

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.specs2.specification.AfterExample

/* A tiny class that can be used as a Specs2 'context'. */
abstract class AkkaTestkitSpecs2Support extends TestKit(ActorSystem())
with AfterExample
with ImplicitSender {
  // make sure we shut down the actor system after all tests have run
  def after = {
    if (!system.isTerminated) {
      system.shutdown
      system.awaitTermination
    }
  }
}