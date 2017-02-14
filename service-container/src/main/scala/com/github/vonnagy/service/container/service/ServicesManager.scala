package com.github.vonnagy.service.container.service

import akka.actor.{Actor, Props, Stash}
import com.github.vonnagy.service.container.health._
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import com.github.vonnagy.service.container.http.{HttpStopped, _}
import com.github.vonnagy.service.container.log.ActorLoggingAdapter
import com.github.vonnagy.service.container.metrics.reporting.MetricsReportingManager

case class StatusRunning()
case class ShutdownService(exit: Boolean = false)

object ServicesManager {
  def props(service: ContainerService,
            routeEndpoints: Seq[Class[_ <: RoutedEndpoints]], props: Seq[Tuple2[String, Props]]): Props =
    Props(classOf[ServicesManager], service, routeEndpoints, props).withDispatcher("akka.actor.service-dispatcher")
}

/**
 * This is the services parent actor that contains all registered services\
 *
 * @param service The main service
 * @param routeEndpoints The routes to manage
 * @param props The collection of registered services to start
 */
class ServicesManager(service: ContainerService,
                      routeEndpoints: Seq[Class[_ <: RoutedEndpoints]], props: Seq[Tuple2[String, Props]]) extends Actor
  with RegisteredHealthCheckActor with Stash with ActorLoggingAdapter {

  // This is a flag so use when the system is explicitly asked to shutdown. `None` means not shutting down,
  // `Some(true)` means call exit after shut down, `Some(false)` means don't call exit after shut down.
  var shutDownAndExit: Option[Boolean] = None

  override def preStart(): Unit = {
    // Start the Http server
    import context.system
    context.actorOf(HttpService.props(routeEndpoints), "http") ! HttpStart
  }

  def receive = initializing

  /**
   * This is the handler when the manager is initializing
    *
    * @return
   */
  def initializing = {
    // Http binding failed so we will change into `stopped` mode. This will cause a CRITICAL health status
    // and thus the service should be restarted.
    case HttpFailed => context.become(stopped)

    case HttpStarted =>
      unstashAll()
      context.become(running)
      log.info("Http service has started")

      // Start the registered services (i.e. Actors)
      startServices

    case GetHealth =>
      sender ! HealthInfo("services", HealthState.DEGRADED, s"The service is currently being initialized")

    case m => stash()

  }: Receive

  /**
   * This is the handler when the manager is running
    *
    * @return
   */
  def running = {
    case StatusRunning => sender.tell(true, self)

    case GetHealth =>
      sender ! HealthInfo("services", HealthState.OK, s"Currently managing ${context.children.toSeq.length} services")

    case HttpStopped =>
      shutDownAndExit = None
      context.become(stopped)

    case ShutdownService(exit) =>
      // Move into the shutting down mode
      shutDownAndExit = Some(exit)
      context.become(shuttingDown)

      context.child("http") match {
        case Some(http) => context.stop(http)
        // There is no Http child running so send ourself the HttpStopped message
        case None => self ! HttpStopped
      }

  }: Receive

  /**
    * This is the handler when the manager is stopping
    *
    * @return
    */
  def shuttingDown = {
    case HttpStopped =>
      service.shutdown()

      shutDownAndExit match {
        case Some(true) => sys.exit()
        case _ =>
      }

    case StatusRunning => sender.tell(true, self)

    case GetHealth =>
      sender ! HealthInfo("services", HealthState.DEGRADED,
        s"Service is stopping. Currently managing ${context.children.toSeq.length} services", None)

  }: Receive

  /**
   * This is the handler when the manager is stopped
    *
    * @return
   */
  def stopped = {
    case StatusRunning => sender.tell(false, self)

    case GetHealth =>
      sender ! HealthInfo("services", HealthState.CRITICAL,
        s"Service is stopped. Currently managing ${context.children.toSeq.length} services", None)

  }: Receive

  /**
   * Start the registered services
   */
  private def startServices(): Unit = {
    // Start the metrics reporters
    context.actorOf(MetricsReportingManager.props())

    // Create the registered services
    props.foreach { p =>
      val act = context.actorOf(p._2, p._1)
      log.info(s"Starting the service ${p._1} at ${act.path}")
    }
  }
}
