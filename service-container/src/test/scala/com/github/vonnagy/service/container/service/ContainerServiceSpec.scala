package com.github.vonnagy.service.container.service

import org.specs2.mutable.Specification

class ContainerServiceSpec extends Specification {

  sequential
  val cont = new ContainerService(Nil, Nil, Nil)

  "The ContainerService" should {

    "create the appropriate parts during construction" in {
      cont.registeredHealthChecks must be equalTo Nil
      cont.registeredRoutes must be equalTo Nil
      cont.started must beFalse
    }

    "shut down properly when asked" in {
      cont.shutdown
      cont.system.isEmpty must beTrue
    }


  }
}
