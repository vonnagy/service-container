package com.github.vonnagy.service.container.service

import org.specs2.mutable.Specification

class ContainerServiceSpec extends Specification {

  "The ContainerService" should {

    "create the appropriate parts during construction" in {

      val cont = new ContainerService(Nil, Nil, Nil)
      cont.registeredHealthChecks must be equalTo Nil
      cont.registeredRoutes must be equalTo Nil
      cont.started must beFalse
    }

    "shut down properly when asked" in {

      val cont = new ContainerService(Nil, Nil, Nil)
      cont.start
      cont.shutdown
      cont.system.get.whenTerminated.isCompleted must beTrue
    }


  }
}
