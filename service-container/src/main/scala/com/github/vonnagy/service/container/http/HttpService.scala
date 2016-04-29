package com.github.vonnagy.service.container.http

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{RoutingLog, Route}
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.github.vonnagy.service.container.health._
import com.github.vonnagy.service.container.http.routing.{RoutedEndpoints, RoutedService}
import com.github.vonnagy.service.container.http.security.SSLProvider
import com.github.vonnagy.service.container.metrics.MetricsEndpoints
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.util.{Success, Failure}

case class Bound(binding: Http.ServerBinding)
case class BindFailure(ex: Throwable)
case class Unbound()
case class UnbindFailure(ex: Throwable)

case class HttpStart()
case class HttpStarted()
case class HttpStopped()

/**
 * The main Http REST service. It handles the Http server and also setups the registered
 * endpoints.
 */
object HttpService {
  def props(routeEndpoints: Seq[Class[_ <: RoutedEndpoints]])(implicit system: ActorSystem): Props = {
    Props(classOf[HttpService], routeEndpoints).withDispatcher("akka.actor.http-dispatcher")
  }
}

class HttpService(routeEndpoints: Seq[Class[_ <: RoutedEndpoints]]) extends Actor with RegisteredHealthCheckActor
  with RoutedService with SSLProvider with BaseDirectives with HttpMetrics {

  import context.dispatcher
  implicit val materializer = ActorMaterializer()(context)

  // Load the server settings and override the spray SSL setting based on what our
  // setting is
  private val spSettings = ServerSettings(ConfigFactory.parseString(s"""spray.can.server.ssl-encryption=${this.sslSettings.enabled}""")
    .withFallback(context.system.settings.config))

  private[http] var httpServer: Option[Http.ServerBinding] = None

  val httpInterface = context.system.settings.config.getString("container.http.interface")
  val port = context.system.settings.config.getInt("container.http.port")

  override def postStop(): Unit = {
    stopHttpServer
  }

  /**
    * The base receive
    *
    * @return
    */
  def receive = {
    // Start the Http server
    case HttpStart => startHttpServer

    case GetHealth => HealthInfo("http", HealthState.DEGRADED,
      s"The http service is currently initializing $httpInterface:$port")

    case Bound(binding) =>
      scheduleHttpMetrics(FiniteDuration(10, TimeUnit.SECONDS))
      httpServer = Some(binding)
      context.become(running)
      context.parent ! HttpStarted

    case BindFailure =>
      log.error(s"Error trying to bind to $httpInterface:$port")
      context.stop(self)
  }

  /**
    * The receive when running
    * @return
    */
  def running = routeReceive orElse {
    case GetHealth => sender ! getHttpHealth()
  }: Receive

  /**
    * Start the http server
    */
  def startHttpServer(): Unit = {

    implicit val timeout = Timeout(2 seconds)
    // Load the routes
    val initialRoutes = routeEndpoints ++
      Seq(classOf[BaseEndpoints], classOf[HealthEndpoints], classOf[MetricsEndpoints])

    val route = loadAndBuildRoute(initialRoutes)

    // a running HttpServer can be bound, unbound and rebound
    // initially to need to tell it where to bind to
    log.info(s"Trying to bind to $httpInterface:$port")

    val bindingFuture = Http().bindAndHandleAsync(Route.asyncHandler(route)(
        routeSettings,
        spSettings.parserSettings,
        materializer,
        RoutingLog.fromActorContext,
        context.dispatcher,
        rejectionHandler),
      httpInterface,
      port,
      httpConnectionContext,
      spSettings)

    // Log our failure to bind
    val me = self
    bindingFuture onComplete  {
      case Success(bind) => me ! Bound(bind)
      case Failure(ex) => me ! BindFailure
    }
  }

  /**
   * Shutdown the Http server
   */
  def stopHttpServer(): Unit = {
    cancelHttpMetrics

    if (httpServer.isDefined) {
      val me = self
      httpServer.get.unbind onComplete {
        case Failure(ex) =>
          log.error(s"Error trying to unbind from $httpInterface:$port", ex)
          context.parent ! HttpStopped
          httpServer = None
        case _ =>
          context.parent ! HttpStopped
          httpServer = None
      }
    }
    else {
      // Send our self an Unbound message anyways
      context.parent ! HttpStopped
      httpServer = None
    }
  }

  /**
   * Get the health of the Http server
    *
    * @return An instance of `HealthInfo`
   */
  def getHttpHealth(): HealthInfo = {
    httpServer.isDefined match {
      case true => HealthInfo("http", HealthState.OK, s"Currently connected on $httpInterface:$port")
      case false => HealthInfo("http", HealthState.CRITICAL, s"Currently not connected on $httpInterface:$port")
    }

  }

}
