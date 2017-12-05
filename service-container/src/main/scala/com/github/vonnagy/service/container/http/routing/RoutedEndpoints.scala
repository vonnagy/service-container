package com.github.vonnagy.service.container.http.routing

import akka.actor.{Actor, ActorSystem}
import akka.http.scaladsl.server.Route
import com.github.vonnagy.service.container.http.{BaseDirectives, DefaultMarshallers}

import scala.concurrent.ExecutionContext

/**
  * Extend this class to provide Http routing for defined routes.
  * @param system the `ActorSystem` for the application
  * @param executor the in-scope executor
  */
abstract class RoutedEndpoints(implicit system: ActorSystem,
                               executor: ExecutionContext) extends BaseDirectives with DefaultMarshallers {

  def route: Route
}


/**
  * Apply this trait to provide Http routing from a specific actor
  */
trait RoutedEndpointsActor extends RoutedEndpoints with Actor {

  /**
    * When the actor is first created it will register itself with the Http service
    */
  protected def registerRoute(): Unit = {
    context.system.actorSelection("user/service/http") ! AddRoute(this)
  }

  // Register the route
  registerRoute

}
