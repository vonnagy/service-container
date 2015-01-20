package com.github.vonnagy.service.container.core

import akka.actor.ActorSystem
import com.github.vonnagy.service.container.service.ContainerService
import org.slf4j.LoggerFactory


/**
 * This trait implements the termination handler to stop the system when the JVM exits.
 */
trait SystemShutdown {

  implicit val system: ActorSystem
  private val shutLog = LoggerFactory.getLogger(this.getClass)

  /**
   * Ensure that the constructed ActorSystem is shut down when the JVM shuts down
   */
  sys.addShutdownHook {
    if (system != null && !system.isTerminated) {
      system.log.info("Shutdown hook called: Shutting down the actor system")
      shutdownActorSystem(system) {
        // Do nothing since the process is shutting down
      }
    }
  }

  /**
   * Shutdown the actor system
   */
  private[container] def shutdownActorSystem(system: ActorSystem)(f: => Unit) = {

    if (system != null && !system.isTerminated) {
      try {
        shutLog.info("Shutting down the actor system")
        system.shutdown
        // Wait for termination if it is not already complete
        system.awaitTermination()
        shutLog.info("The actor system has terminated")
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
