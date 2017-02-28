package com.github.vonnagy.service.container.service

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.vonnagy.service.container.core.{CoreConfig, SystemShutdown}
import com.github.vonnagy.service.container.health.{Health, HealthCheck}
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import com.github.vonnagy.service.container.listener.ContainerLifecycleListener
import com.github.vonnagy.service.container.log.LoggingAdapter
import com.github.vonnagy.service.container.service.ServicesManager.StatusRunning
import com.typesafe.config.Config
import configs.syntax._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * This is the main class for the container. It takes several parameters to be used in its construction.
  *
  * @param routeEndpoints a list of route classes to manage
  * @param healthChecks   a list of health checks to register in the system
  * @param props          a lost of actor props to create when starting the container
  * @param name The name of this container service
  */
class ContainerService(routeEndpoints: Seq[Class[_ <: RoutedEndpoints]] = Nil,
                       healthChecks: Seq[HealthCheck] = Nil,
                       props: Seq[Tuple2[String, Props]] = Nil,
                       val listeners: Seq[ContainerLifecycleListener] = Nil,
                       val name: String)(implicit val system: ActorSystem)
  extends CoreConfig with SystemShutdown with LoggingAdapter {

  var started = false

  // Create the root actor that all services will run under
  lazy val servicesParent = system.actorOf(ServicesManager.props(this, routeEndpoints, props), "service")

  /**
    * Start the service which will start the build-in Http service and
    * instantiate all of the desired Actors, etc...
    */
  def start(): Unit = {

    if (!started) {
      started = true
      // Update the health check registry
      healthChecks.foreach(Health(system).addCheck(_))

      // Only block here since we are starting the system
      val td = system.settings.config.get[FiniteDuration]("container.startup.timeout").valueOrElse(5 seconds)
      implicit val timeout = Timeout(td)
      Await.result(servicesParent ? StatusRunning, td)
      log.info(s"Container $name has been started.")
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
  def registeredHealthChecks(): Seq[HealthCheck] = if (started) Health(system).getChecks else healthChecks

}