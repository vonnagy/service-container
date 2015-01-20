package com.github.vonnagy.service.container.http

import java.util.concurrent.TimeUnit

import akka.ConfigurationException
import akka.actor.{Actor, ActorRefFactory, ActorSystem, ExtendedActorSystem}
import akka.io.{IO, Tcp}
import akka.routing.FromConfig
import akka.util.Timeout
import com.github.vonnagy.service.container.health.{HealthEndpoints, HealthInfo, HealthState}
import com.github.vonnagy.service.container.http.routing.{RoutedEndpoints, RoutedService}
import com.github.vonnagy.service.container.metrics.MetricsEndpoints
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
trait HttpService extends RouteConcatenation with HttpMetrics {
  this: Actor =>

  implicit def system = context.system

  val httpInterface: String
  val port: Int

  private val spSettings = ServerSettings(context.system.settings.config)
  private val httpServer = IO(Http)

  val httpListener = context.system.actorSelection(httpServer.path.toString.concat("/listener-0"))

  var httpBound = false

  val httpStarting: Receive = {
    case Tcp.Bound(_) =>
      scheduleHttpMetrics(FiniteDuration(10, TimeUnit.SECONDS))
      httpBound = true
      self ! HttpStarted

    case Http.Connected => sender ! Http.Register(self)

    case Tcp.CommandFailed(_: Http.Bind) =>
      log.error(s"Error trying to bind to $httpInterface:$port")
      context.stop(self)
  }

  val httpStopping: Receive = {
    case Tcp.Unbound =>
      httpBound = false
      system.stop(httpServer)
      self ! HttpStopped
    case Tcp.CommandFailed(_: Http.Unbind) =>
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

    httpServer ! Http.Bind(listener = httpService,
      interface = httpInterface,
      port = port,
      settings = Some(spSettings))
  }

  /**
   * Shutdown the Http server
   */
  def stopHttpServer: Unit = {
    cancelHttpMetrics
    httpListener ! Http.Unbind
  }

  /**
   * Get the health of the Http server
   * @return An instance of `HealthInfo`
   */
  def getHttpHealth: HealthInfo = {
    httpBound match {
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
        //          ct.newInstance()
        //          val c = currentMirror.staticClass(route.getName)
        //          val cm = currentMirror.reflectClass(c)
        //          val t = c.selfType
        //          val ctor = t.declaration(ru.nme.CONSTRUCTOR).asMethod
        //          val ctorm = cm.reflectConstructor(ctor)
        //          val args = ctor.asMethod.paramss.head map { p => (p.name.decoded, p.typeSignature) }

        //val p = ctorm(context.system)
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
