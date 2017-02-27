package com.github.vonnagy.service.container.core

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.github.vonnagy.service.container.log.LoggingAdapter
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.sys.ShutdownHookThread


/**
  * This trait implements the termination handler to stop the system when the JVM exits.
  */
trait SystemShutdown extends LoggingAdapter {

  def system: ActorSystem

  /**
    * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
    */
  var shutdownHook: Option[ShutdownHookThread] = Some(sys.addShutdownHook {
    log.info("Shutdown hook called: Shutting down the actor system")
    shutdownActorSystem(true) {}

  })

  /**
    * Shutdown the actor system
    */
  private[container] def shutdownActorSystem(fromHook: Boolean = false)(f: => Unit): Unit = {

    try {
      // Remove the hook
      if (shutdownHook.isDefined && !fromHook) {
        shutdownHook.get.remove

      }
      shutdownHook = None

      log.info("Shutting down the actor system")
      system.terminate()

      // Wait for termination if it is not already complete
      Await.result(system.whenTerminated, Duration.apply(30, TimeUnit.SECONDS))
      log.info("The actor system has terminated")
    }
    catch {
      case t: Throwable =>
        log.error(s"The actor system could not be shutdown: ${t.getMessage}", t)
    }

    // Call the passed function
    f
  }
}
