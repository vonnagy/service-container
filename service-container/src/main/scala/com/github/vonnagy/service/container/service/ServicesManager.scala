package com.github.vonnagy.service.container.service

import scala.util.Properties
import akka.actor.{Actor, Props, Stash}
import com.github.vonnagy.service.container.health._
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import com.github.vonnagy.service.container.http.{HttpService, HttpStarted, HttpStopped}
import com.github.vonnagy.service.container.metrics.reporting.MetricsReportingManager

case class StatusRunning()

object ServicesManager {
  def props(routeEndpoints: Seq[Class[_ <: RoutedEndpoints]], props: Seq[Tuple2[String, Props]]): Props =
    Props(classOf[ServicesManager], routeEndpoints, props).withDispatcher("akka.actor.service-dispatcher")
}

/**
 * This is the services parent actor that contains all registered services
 * @param routeEndpoints The routes to manage
 * @param props The collection of registered services to start
 */
class ServicesManager(routeEndpoints: Seq[Class[_ <: RoutedEndpoints]], props: Seq[Tuple2[String, Props]]) extends Actor
with HttpService with RegisteredHealthCheckActor with Stash {

  val httpInterface = context.system.settings.config.getString("container.http.interface")
  val port = Properties.envOrElse("PORT", context.system.settings.config.getInt("container.http.port").toString()).toInt

  override def preStart(): Unit = {
    // Start the Http server
    startHttpServer(routeEndpoints)
  }

  override def postStop(): Unit = {
    stopHttpServer
  }

  def receive = initializing

  /**
   * This is the handler when the manager is initializing
   * @return
   */
  def initializing = httpStarting orElse {
    case HttpStarted =>
      unstashAll()
      context.become(running)
      log.info("Http service has started")

      // Start the registered services (i.e. Actors)
      startServices

    case GetHealth =>
      sender ! HealthInfo("services", HealthState.DEGRADED, s"The Http service is currently being initialized")

    case m => stash()

  }: Receive

  /**
   * This is the handler when the manager is running
   * @return
   */
  def running = httpStopping orElse {
    case StatusRunning => sender.tell(true, self)

    case GetHealth =>
      sender ! HealthInfo("services", HealthState.OK,
        s"Currently managing ${context.children.toSeq.length} services (including http)", None, List(getHttpHealth))

    case HttpStopped => context.become(stopped)

  }: Receive

  /**
   * This is the handler when the manager is stopped
   * @return
   */
  def stopped = {
    case StatusRunning => sender.tell(false, self)

    case GetHealth =>
      sender ! HealthInfo("services", HealthState.CRITICAL,
        s"The Http service is NOT running. It is currently managing ${context.children.toSeq.length} services (including http)", None, List(getHttpHealth))

  }: Receive

  /**
   * Start the registered services
   */
  private def startServices(): Unit = {
    // Start the metrics reporters
    context.actorOf(MetricsReportingManager.props())

    // Create the registered services
    props.foreach { p =>
      log.info("Starting the service {}", p._1)
      context.actorOf(p._2, p._1)
    }
  }
}
