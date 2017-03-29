package com.github.vonnagy.service.container.http.routing

import akka.ConfigurationException
import akka.actor._
import akka.http.scaladsl.server.{RouteConcatenation, Route}

/**
 * Add a set of defined routes
  *
  * @param route
 */
case class AddRoute(route: RoutedEndpoints)

/**
 * This message is sent back to the sender when the route has been officially added
 */
case class RouteAdded()

/**
 * Get a sequence of defined routes
 */
case class GetRoutes()

/**
 * This is the return from the message ``GetRoutes``
  *
  * @param routes the currently defined routes
 */
case class Routes(routes: Seq[RoutedEndpoints])


/**
 * Allows you to construct Spray ``HttpService`` from a concatenation of routes; and wires in the error handler.
 * It also logs all internal server errors using ``ActorLoggingAdapter``.
 */
trait RoutedService extends RoutingHandler with RouteConcatenation {
  this: Actor =>

  def conf = context.system.settings.config
  private[routing] var routes =  Seq[RoutedEndpoints]()

  // The base handler
  val routeReceive: Receive = {
    case AddRoute(route) => addRoute(route); sender ! RouteAdded
    case GetRoutes => sender ! Routes(routes)
  }

  /**
    * Load the designated routes and store them for later
    * @param routeEndpoints
    */
  def loadAndBuildRoute(routeEndpoints: Seq[Class[_ <: RoutedEndpoints]]): Route = {
    routes = loadRoutes(routeEndpoints)
    buildRoute(routes)
  }

  /**
    * Build the routes from sequence of ``RoutedEndpoints``
    *
    * @param services the the service that will be used to build the routes
    * @return an instance of ``Route``
    */
  private[routing] def buildRoute(services: Iterable[RoutedEndpoints]): Route = {
    services.map(_.route).reduceLeft(_ ~ _)
  }

  /**
    * Add the route and reset the message handler
    *
    * @param route the route to add
    */
  private[routing] def addRoute(route: RoutedEndpoints): Unit = {
    routes = routes ++ Seq(route)
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
