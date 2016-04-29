package com.github.vonnagy.service.container.core

import akka.actor.ActorSystem
import org.specs2.mutable.Specification

/**
 * Created by Ivan von Nagy on 1/19/15.
 */
class SystemShutdownSpec extends Specification {

  "SystemShutdown" should {

    "allow the ActorSystem to be shutdown" in {
      val sys = Some(ActorSystem())
      val shut = new SystemShutdown {}

      shut.shutdownActorSystem(sys) {}
      sys.get.whenTerminated.isCompleted must beTrue
    }
  }
}
