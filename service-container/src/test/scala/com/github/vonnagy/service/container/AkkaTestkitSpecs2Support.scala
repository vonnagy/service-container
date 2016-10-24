package com.github.vonnagy.service.container

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.specs2.specification.AfterAll

/* A tiny class that can be used as a Specs2 'context'. */
abstract class AkkaTestkitSpecs2Support(sys: ActorSystem = ActorSystem()) extends TestKit(sys)
  with AfterAll
  with ImplicitSender {
  // make sure we shut down the actor system after all tests have run
  def afterAll = {
    TestKit.shutdownActorSystem(sys, verifySystemShutdown = true)
  }
}