package com.github.vonnagy.service.container.http

import java.util.concurrent.TimeUnit

import akka.ConfigurationException
import akka.actor._
import akka.io.IO
import akka.routing.FromConfig
import akka.util.Timeout
import com.github.vonnagy.service.container.health.{HealthEndpoints, HealthInfo, HealthState}
import com.github.vonnagy.service.container.http.routing.{RoutedEndpoints, RoutedService}
import com.github.vonnagy.service.container.http.security.SSLProvider
import com.github.vonnagy.service.container.metrics.MetricsEndpoints
import com.typesafe.config.ConfigFactory
import spray.can.Http
import spray.can.server.ServerSettings
import spray.routing.RouteConcatenation

import scala.concurrent.duration._
import scala.reflect.runtime.{universe => ru}

case class HttpStarted()

case class HttpStopped()

/**
 * The main Http REST service. It handles the Http server and also setups the registered
 * endpoints.
 */
trait HttpService extends RouteConcatenation with HttpMetrics with SSLProvider {
  this: Actor =>

  implicit def system = context.system

  val httpInterface: String
  val port: Int

  // Load the server settings and override the spray SSL setting based on what our
  // setting is
  private val spSettings = ServerSettings(ConfigFactory.parseString(s"""spray.can.server.ssl-encryption=${this.sslSettings.enabled}""")
    .withFallback(context.system.settings.config))

  private val httpServer = IO(Http)

  var httpListener: Option[ActorSelection] = None

  val httpStarting: Receive = {

    case Http.Bound(_) =>
      scheduleHttpMetrics(FiniteDuration(10, TimeUnit.SECONDS))
      httpListener = Some(context.system.actorSelection(sender.path))
      self ! HttpStarted

    case Http.CommandFailed(_: Http.Bind) =>
      log.error(s"Error trying to bind to $httpInterface:$port")
      context.stop(self)
  }

  val httpStopping: Receive = {
    case Http.Unbound =>
      httpListener = None
      system.stop(httpServer)
      self ! HttpStopped
    case Http.CommandFailed(_: Http.Unbind) =>
      log.error(s"Error trying to unbind from $httpInterface:$port")
  }

  /**
   * Start the http server
   */
  def startHttpServer(routes: Seq[Class[_ <: RoutedEndpoints]]): Unit = {

    implicit val timeout = Timeout(2 seconds)
    val loadedRoutes = loadRoutes(routes ++ Seq(classOf[BaseEndpoints], classOf[HealthEndpoints], classOf[MetricsEndpoints]))
    val httpService = context.actorOf(FromConfig.props(RoutedService.props(loadedRoutes)), "http")

    // a running HttpServer can be bound, unbound and rebound
    // initially to need to tell it where to bind to
    log.info(s"Trying to bind to $httpInterface:$port")

    val bind = Http.Bind(listener = httpService,
      interface = httpInterface,
      port = port,
      settings = Some(spSettings))

    httpServer ! bind
  }

  /**
   * Shutdown the Http server
   */
  def stopHttpServer(): Unit = {
    cancelHttpMetrics
    if (httpListener.isDefined) httpListener.get ! Http.Unbind
  }

  /**
   * Get the health of the Http server
   * @return An instance of `HealthInfo`
   */
  def getHttpHealth(): HealthInfo = {
    httpListener.isDefined match {
      case true => HealthInfo("http", HealthState.OK, s"Currently connected on $httpInterface:$port")
      case false => HealthInfo("http", HealthState.CRITICAL, s"Currently not connected on $httpInterface:$port")
    }

  }

  /**
   * Load the defined routes
   */
  private def loadRoutes(routeEndpoints: Seq[Class[_ <: RoutedEndpoints]]): Seq[RoutedEndpoints] = {

    log.info("Setting up all of the routes")
    val newRoutes =
      for {
        route <- routeEndpoints
      } yield {
        val ct = route.getConstructors.last
        val params = ct.getParameterTypes

        val p = params.length match {
          case 0 => Nil
          case _ => List(classOf[ActorSystem] -> context.system)
        }
        val args = List(classOf[ActorSystem] -> context.system, classOf[ActorRefFactory] -> context)

        context.system.asInstanceOf[ExtendedActorSystem].dynamicAccess
          .createInstanceFor[RoutedEndpoints](route.getName, args).map({
          case route =>
            route
        }).recover({
          case e => throw new ConfigurationException(
            "RoutedEndpoints can't be loaded [" + route.getName +
              "] due to [" + e.toString + "]", e)
        }).get
      }

    newRoutes.toSeq
  }

}
