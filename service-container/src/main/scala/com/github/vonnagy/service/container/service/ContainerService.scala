package com.github.vonnagy.service.container.service

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.vonnagy.service.container.core.{CoreConfig, SystemShutdown}
import com.github.vonnagy.service.container.health.{Health, HealthCheck}
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import com.github.vonnagy.service.container.log.LoggingAdapter
import com.typesafe.config.Config

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * This is the main class for the container. It takes several parameters to be used in its construction.
 * @param routeEndpoints a list of route classes to manage
 * @param healthChecks a list of health checks to register in the system
 * @param props a lost of actor props to create when starting the container
 * @param config an optional configuration to use. It will tak precedence over those pass from the command line or
 *               from the default.
 */
class ContainerService(routeEndpoints: Seq[Class[_ <: RoutedEndpoints]] = Nil,
                                 healthChecks: Seq[HealthCheck] = Nil,
                                 props: Seq[Tuple2[String, Props]] = Nil,
                                 config: Option[Config] = None) extends CoreConfig with SystemShutdown with LoggingAdapter {

  // ActorSystem we will use in our application
  lazy val system = ActorSystem.create("server", getConfig(config))
  var started = false

  /**
   * Start the service which will start the build-in Http service and
   * instantiate all of the desired Actors, etc...
   */
  def start: Unit = {

    if (!started && !system.isTerminated) {
      started = true

      // Update the health check registry
      healthChecks.foreach(Health(system).addCheck(_))

      // Create the root actor that all services will run under
      val servicesParent = system.actorOf(ServicesManager.props(routeEndpoints, props), "service")

      // Only block here since we are starting the system
      implicit val timeout = Timeout(5 seconds)
      Await.result(servicesParent ? StatusRunning, 5 seconds)
      log.info("The container service has been started")
    }
    else if (system.isTerminated) {
      log.error("The container service has already been stopped")
    }
  }

  /**
   * This will shutdown the system and wait until it has completed
   * the shutdown process.
   */
  def shutdown: Unit = {
    if (started && !system.isTerminated) {
      shutdownActorSystem(system) {
        // Do nothing
      }
    }
    started = false
  }

  /**
   * Get the system's registered endpoint handlers
   * @return a list of ``RoutedEndpoints``
   */
  def registeredRoutes: Seq[Class[_ <: RoutedEndpoints]] = routeEndpoints

  /**
   * Get the system's registered health checks
   * @return a list of ``HealthCheck``
   */
  def registeredHealthChecks: Seq[HealthCheck] = if (started) Health(system).getChecks else healthChecks

}