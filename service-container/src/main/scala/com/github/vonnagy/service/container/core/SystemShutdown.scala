package com.github.vonnagy.service.container.core

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.sys.ShutdownHookThread


/**
  * This trait implements the termination handler to stop the system when the JVM exits.
  */
trait SystemShutdown {

  var system: Option[ActorSystem] = None
  private val shutLog = LoggerFactory.getLogger(this.getClass)

  /**
    * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
    */
  var shutdownHook: Option[ShutdownHookThread] = Some(sys.addShutdownHook {
    if (system.isDefined) {
      shutLog.info("Shutdown hook called: Shutting down the actor system")
      shutdownActorSystem(true) {}
    }
  })

  /**
    * Shutdown the actor system
    */
  private[container] def shutdownActorSystem(fromHook: Boolean = false)(f: => Unit): Unit = {

    if (system.isDefined) {
      try {
        // Remove the hook
        if (shutdownHook.isDefined && !fromHook) {
          shutdownHook.get.remove

        }
        shutdownHook = None

        shutLog.info("Shutting down the actor system")
        system.get.terminate()
        // Wait for termination if it is not already complete
        Await.result(system.get.whenTerminated, Duration.apply(30, TimeUnit.SECONDS))
        shutLog.info("The actor system has terminated")
        system = None
      }
      catch {
        case t: Throwable =>
          shutLog.error(s"The actor system could not be shutdown: ${t.getMessage}", t)
      }
    }

    // Call the passed function
    f
  }
}
