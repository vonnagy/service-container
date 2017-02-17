package com.github.vonnagy.service.container.service

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.vonnagy.service.container.core.{CoreConfig, SystemShutdown}
import com.github.vonnagy.service.container.health.{Health, HealthCheck}
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import com.github.vonnagy.service.container.listener.ContainerLifecycleListener
import com.github.vonnagy.service.container.log.LoggingAdapter
import com.github.vonnagy.service.container.service.ServicesManager.{FindService, StatusRunning}
import com.typesafe.config.Config
import configs.syntax._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * This is the main class for the container. It takes several parameters to be used in its construction.
  *
  * @param routeEndpoints a list of route classes to manage
  * @param healthChecks   a list of health checks to register in the system
  * @param props          a lost of actor props to create when starting the container
  * @param config         an optional configuration to use. It will tak precedence over those pass from the command line or
  *                       from the default.
  */
class ContainerService(routeEndpoints: Seq[Class[_ <: RoutedEndpoints]] = Nil,
                       healthChecks: Seq[HealthCheck] = Nil,
                       props: Seq[Tuple2[String, Props]] = Nil,
                       val listeners: Seq[ContainerLifecycleListener] = Nil,
                       config: Option[Config] = None) extends CoreConfig with SystemShutdown with LoggingAdapter {

  // ActorSystem we will use in our application
  system = Some(ActorSystem.create("server", getConfig(config)))
  var started = false

  // Create the root actor that all services will run under
  lazy val servicesParent = system.get.actorOf(ServicesManager.props(this, routeEndpoints, props), "service")

  /**
    * Start the service which will start the build-in Http service and
    * instantiate all of the desired Actors, etc...
    */
  def start(): Unit = {

    if (!started) {
      started = true
      // Update the health check registry
      healthChecks.foreach(Health(system.get).addCheck(_))

      // Only block here since we are starting the system
      val td = getConfig(config).get[FiniteDuration]("container.startup.timeout").valueOrElse(5 seconds)
      implicit val timeout = Timeout(td)
      Await.result(servicesParent ? StatusRunning, td)
      log.info("The container service has been started.")
      listeners.foreach(_.onStartup(this))
      sys.addShutdownHook(listeners.foreach(_.onShutdown(this)))
    }

  }

  /**
    * This will shutdown the system and wait until it has completed
    * the shutdown process.
    */
  def shutdown(): Unit = {
    shutdownActorSystem(false) {
      // Do nothing
    }
    started = false
  }

  /**
    * Gets a registered service by its name.
    *
    * @return an Option containing the ActorRef of the service or None if not found.
    */
  def findService(name: String): Future[Option[ActorRef]] = {
    implicit val timeout = Timeout(1 second)
    (servicesParent ? FindService(name)).mapTo[Option[ActorRef]]
  }

  /**
    * Get the system's registered endpoint handlers
    *
    * @return a list of ``RoutedEndpoints``
    */
  def registeredRoutes(): Seq[Class[_ <: RoutedEndpoints]] = routeEndpoints

  /**
    * Get the system's registered health checks
    *
    * @return a list of ``HealthCheck``
    */
  def registeredHealthChecks(): Seq[HealthCheck] = if (started) Health(system.get).getChecks else healthChecks

}