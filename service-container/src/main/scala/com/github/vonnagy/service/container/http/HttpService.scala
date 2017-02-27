package com.github.vonnagy.service.container.http

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.{Route, RoutingLog}
import akka.http.scaladsl.settings.ServerSettings
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.github.vonnagy.service.container.health._
import com.github.vonnagy.service.container.http.routing.{RoutedEndpoints, RoutedService}
import com.github.vonnagy.service.container.http.security.SSLProvider
import com.github.vonnagy.service.container.metrics.MetricsEndpoints

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class Bound(binding: Seq[Http.ServerBinding])
case class BindFailure(ex: Throwable)
case class Unbound()
case class UnbindFailure(ex: Throwable)

case class HttpStart()
case class HttpStop()
case class HttpStarted()
case class HttpStopped()
case class HttpFailed()

/**
 * The main Http REST service. It handles the Http server and also setups the registered
 * endpoints.
 */
object HttpService {
  def props(routeEndpoints: Seq[Class[_ <: RoutedEndpoints]])(implicit system: ActorSystem): Props = {
    Props(classOf[HttpService], routeEndpoints)
  }
}

class HttpService(routeEndpoints: Seq[Class[_ <: RoutedEndpoints]]) extends Actor with RegisteredHealthCheckActor
  with RoutedService with SSLProvider with BaseDirectives with HttpMetrics {

  import context.dispatcher
  implicit val materializer = ActorMaterializer()(context)

  // Load the server settings
  private val spSettings = ServerSettings(context.system.settings.config)

  private[http] var httpServer: Seq[Http.ServerBinding] = Nil

  val httpSettings = (context.system.settings.config.getString("container.http.port") match {
      case "disabled" => Nil
      case p => Seq((context.system.settings.config.getString("container.http.interface"), p.toInt))
    }) ++
    (context.system.settings.config.getString("container.https.port") match {
      case "disabled" => Nil
      case p => Seq((context.system.settings.config.getString("container.https.interface"), p.toInt))
    })

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
      s"The http service is currently initializing on ${httpSettings.map(h => s"${h._1}:${h._2}").mkString(", ")}")

    case Bound(binding) =>
      scheduleHttpMetrics(FiniteDuration(10, TimeUnit.SECONDS))
      httpServer = binding
      log.info(s"Http server is bound on ${httpServer.map(_.localAddress).mkString(", ")}")
      context.become(running)
      context.parent ! HttpStarted

    case BindFailure(ex) =>
      // Binding failed so notify the parent (ServicesManager) that Http has failed.
      log.error(s"Error trying to bind", ex)
      context.parent ! HttpFailed
  }

  /**
    * The receive when running
    * @return
    */
  def running = routeReceive orElse {
    case HttpStop => stopHttpServer()
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

    val future = Future.traverse(httpSettings) { http =>
      bindHttp(http._1, http._2, false, route)
    }

    // Log our failure to bind
    val me = self
    future onComplete {
      case Success(bind) => me ! Bound(bind)
      case Failure(ex) => me ! BindFailure(ex)
    }

  }

  /**
   * Shutdown the Http server
   */
  def stopHttpServer(): Unit = {
    cancelHttpMetrics

    if (!httpServer.isEmpty) {
      val future = Future.traverse(httpServer) { binding => binding.unbind }
      val ctx = context

      future onComplete {
        case Failure(ex) =>
          log.error(s"Error trying to unbind", ex)
          httpServer = Nil
          ctx.parent ! HttpStopped

        case _ =>
          log.info(s"Http server is now unbound from ${httpServer.map(_.localAddress).mkString(", ")}")
          httpServer = Nil
          ctx.parent ! HttpStopped
      }
    }
  }

  /**
   * Get the health of the Http server
    *
    * @return An instance of `HealthInfo`
   */
  def getHttpHealth(): HealthInfo = {
    httpServer.isEmpty match {
      case false => HealthInfo("http", HealthState.OK,
        s"Currently connected on ${httpServer.map(_.localAddress).mkString(", ")}")

      case true => HealthInfo("http", HealthState.CRITICAL,
        s"Currently not connected on ${httpSettings.map(h => s"${h._1}:${h._2}").mkString(", ")}")
    }

  }

  def bindHttp(interface: String, port: Int, ssl: Boolean, route: Route): Future[ServerBinding] = {
    // a running HttpServer can be bound, unbound and rebound
    // initially to need to tell it where to bind to
    log.info(s"Trying to bind to $interface:$port")

    Http().bindAndHandleAsync(Route.asyncHandler(route)(
      routeSettings,
      spSettings.parserSettings,
      materializer,
      RoutingLog.fromActorContext,
      context.dispatcher,
      rejectionHandler,
      exceptionHandler),
      interface,
      port,
      getContext(ssl),
      spSettings)

  }
}
