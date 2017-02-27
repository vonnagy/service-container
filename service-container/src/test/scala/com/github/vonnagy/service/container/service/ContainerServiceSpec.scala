package com.github.vonnagy.service.container.service

import akka.actor.{ActorSystem, Terminated}
import akka.testkit.TestKit
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike

class ContainerServiceSpec extends TestKit(ActorSystem("service-container")) with SpecificationLike {

  sequential
  val cont = new ContainerService(Nil, Nil, Nil, name = "test")

  "The ContainerService" should {

    "create the appropriate parts during construction" in {
      cont.registeredHealthChecks must be equalTo Nil
      cont.registeredRoutes must be equalTo Nil
      cont.started must beFalse
    }

    "shut down properly when asked" in {
      cont.shutdown
      implicit val ec = ExecutionEnv.fromExecutionContext(system.dispatcher)
      cont.system.whenTerminated must beAnInstanceOf[Terminated].await
    }
  }
}
