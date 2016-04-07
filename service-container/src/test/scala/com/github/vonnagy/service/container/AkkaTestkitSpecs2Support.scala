package com.github.vonnagy.service.container

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.specs2.specification.AfterAll

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/* A tiny class that can be used as a Specs2 'context'. */
abstract class AkkaTestkitSpecs2Support(sys: ActorSystem = ActorSystem()) extends TestKit(sys)
with AfterAll
with ImplicitSender {
  // make sure we shut down the actor system after all tests have run
  def afterAll = {
    Await.result(system.terminate(), Duration(2, TimeUnit.SECONDS))
  }
}