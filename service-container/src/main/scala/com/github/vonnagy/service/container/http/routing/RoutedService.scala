package com.github.vonnagy.service.container.http.routing

import akka.actor.{Actor, ActorSystem, Props}
import com.github.vonnagy.service.container.log.ActorLoggingAdapter
import spray.routing._
import spray.util.LoggingContext

/**
 * Add a set of defined routes
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
 * @param routes the currently defined routes
 */
case class Routes(routes: Seq[RoutedEndpoints])


object RoutedService {
  def props(routeEndpoints: Seq[RoutedEndpoints])(implicit system: ActorSystem): Props = Props(classOf[RoutedService], routeEndpoints)
}

/**
 * Allows you to construct Spray ``HttpService`` from a concatenation of routes; and wires in the error handler.
 * It also logs all internal server errors using ``ActorLoggingAdapter``.
 *
 * @param routeEndpoints the routes to manage
 */
class RoutedService(val routeEndpoints: Seq[RoutedEndpoints]) extends Actor
  with RoutingHandler with HttpServiceBase {

  implicit val fact = context.system
  private[routing] var routes = routeEndpoints

  // The base handler
  val base: Receive = {
    case AddRoute(route) => addRoute(route); sender ! RouteAdded
    case GetRoutes => sender ! Routes(routes.toSeq)
  }

  // The default receive is the base with the services
  def receive = applyRoute(buildRoute(routes)) orElse base

  /**
   * Build the routes from sequence of ``RoutedEndpoints``
   * @param services the the service that will be used to build the routes
   * @return an instance of ``Actor.Receive``
   */
  private[routing] def buildRoute(services: Iterable[RoutedEndpoints]): Route = {
    services.map(_.route).reduceLeft(_ ~ _)
  }

  /**
   * Add the route and reset the message handler
   * @param route the route to add
   */
  private[routing] def addRoute(route: RoutedEndpoints): Unit = {
    routes = routes ++ Seq(route)
    context.become(base orElse applyRoute(buildRoute(routes)))
  }

  /**
   * Apply the route to create an ``Actor.Receive`` PF.
   * @param route
   * @return an instance of ``Actor.Receive``
   */
  private def applyRoute(route: Route): Actor.Receive = {
    runRoute(route)(exceptionHandler, rejectionHandler, context, RoutingSettings.default, LoggingContext.fromActorRefFactory)
  }

}
