package com.github.vonnagy.service.container.core

import akka.actor.{ActorSystem, Terminated}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

/**
  * Created by Ivan von Nagy on 1/19/15.
  */
class SystemShutdownSpec extends Specification {

  "SystemShutdown" should {

    "allow the ActorSystem to be shutdown" in {
      val sys = ActorSystem()
      val shut = new SystemShutdown {
        val system = sys
      }

      shut.shutdownActorSystem(false) {}
      implicit val ec = ExecutionEnv.fromExecutionContext(sys.dispatcher)
      shut.system.whenTerminated must beAnInstanceOf[Terminated].await

      sys.whenTerminated.isCompleted must beTrue
      success
    }
  }
}
