package com.github.vonnagy.service.container.service

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Stash}
import akka.pattern.ask
import akka.util.Timeout
import com.github.vonnagy.service.container.health._
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import com.github.vonnagy.service.container.http.{HttpStopped, _}
import com.github.vonnagy.service.container.log.ActorLoggingAdapter
import com.github.vonnagy.service.container.metrics.reporting.MetricsReportingManager

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ServicesManager {

  def props(service: ContainerService,
            routeEndpoints: Seq[Class[_ <: RoutedEndpoints]], props: Seq[(String, Props)]): Props =
    Props(classOf[ServicesManager], service, routeEndpoints, props).withDispatcher("akka.actor.service-dispatcher")

  /**
    * Looks up a service in the currently materialized ServicesManager.
    *
    * @param name                The name of the service.
    * @param servicesManagerPath The service manager actor path in the system.
    *
    *                            The result is returned as a Future that is completed with the [[ActorRef]]
    *                            if such a service exists. It is completed with failure [[ServiceNotFound]] if
    *                            no such service(actor) with that name exists
    *                            or if there is no service manager in the path specified.
    */
  def findService(name: String, servicesManagerPath: String = "server/user/service")
                 (implicit system: ActorSystem): Future[ActorRef] = {
    implicit val timeout = Timeout(1 second)
    implicit val ec = system.dispatcher
    val serviceManager = system.actorSelection(s"akka://$servicesManagerPath")

    //we need to resolve so that the future resolves to a Failure (and not to a dead letter queue)
    serviceManager.resolveOne().flatMap(ref => (ref ? FindService(name))
      .mapTo[Option[ActorRef]]).flatMap { svc =>
      svc match {
        case Some(act) => Future.successful(act)
        case None => Future.failed(ServiceNotFound(name))
      }
    }
  }

  case object StatusRunning

  case class ShutdownService(exit: Boolean = false)

  case class FindService(name: String)

  final case class ServiceNotFound(name: String) extends RuntimeException(s"Service '$name' not found.")

}

/**
  * This is the services parent actor that contains all registered services\
  *
  * @param service        The main service
  * @param routeEndpoints The routes to manage
  * @param props          The collection of registered services to start
  */
class ServicesManager(service: ContainerService,
                      routeEndpoints: Seq[Class[_ <: RoutedEndpoints]], props: Seq[(String, Props)]) extends Actor
  with RegisteredHealthCheckActor with Stash with ActorLoggingAdapter {

  import com.github.vonnagy.service.container.service.ServicesManager._

  // This is a flag so use when the system is explicitly asked to shutdown. `None` means not shutting down,
  // `Some(true)` means call exit after shut down, `Some(false)` means don't call exit after shut down.
  var shutDownAndExit: Option[Boolean] = None

  //add the internal services to the ones provided by the user
  private val fprops = props :+ ("metrics_reporting_manager" -> MetricsReportingManager.props())

  // We make this a var so that we can initialize it from preStart()
  @volatile
  private var services: Map[String, ActorRef] = _

  override def preStart(): Unit = {
    import context.dispatcher

    //the services have to be started before the HTTP endpoints so that service look ups via FindService are successful.
    initializeServices() onComplete {
      case Success(svcs) =>
        this.services = svcs
        log.info(s"Initialized ${services.size} services.")
        initializeHttpServer
      case Failure(ex) =>
        //don't start
        throw ex
    }

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

    case GetHealth =>
      sender ! HealthInfo("services", HealthState.DEGRADED, s"The service is currently being initialized")

    case _ => stash()

  }: Receive

  /**
    * This is the handler when the manager is running
    *
    * @return
    */
  def running = {
    case StatusRunning => sender.tell(true, self)

    case FindService(name) =>
      sender ! services.get(name)

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
        case Some(http) => http ! HttpStop
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
      // Make sure the shutdown runs in a different thread to prevent deadlocking
      implicit val executor: ExecutionContext = context.system.dispatcher
      context.system.scheduler.scheduleOnce(Duration.Zero) {
        service.shutdown()

        shutDownAndExit match {
          case Some(true) => sys.exit()
          case _ =>
        }
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

  def initializeHttpServer() = {
    context.actorOf(HttpService.props(routeEndpoints), "http") ! HttpStart // And then Start the Http server
  }

  private def initializeServices()(implicit ec: ExecutionContext): Future[Map[String, ActorRef]] = {
    Future {
      Map(fprops.map { p =>
        val svc = context.actorOf(p._2, p._1)
        log.info(s"Started the service ${p._1} at ${svc.path}")
        p._1 -> svc
      }: _*)
    }
  }
}
