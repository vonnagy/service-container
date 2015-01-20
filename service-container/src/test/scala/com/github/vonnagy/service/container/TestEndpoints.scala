package com.github.vonnagy.service.container

import akka.actor.{ActorRefFactory, ActorSystem, Props}
import com.github.vonnagy.service.container.http.routing._
import spray.http.StatusCodes


class TestEndpoints(implicit system: ActorSystem,
                    actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints {

  def getHandler: Props = Props[TestHandler]

  val route = {
    path("test") {
      respondPlain {
        complete("test")
      }
    } ~
      path("test-per-request") {
        respondPlain {
          ctx =>
            perRequest[String](ctx, getHandler, new RestRequest {})
        }
      }
  }
}

class TestHandler extends PerRequestHandler {

  def receive = {
    case _ => response("test-complete", StatusCodes.OK)
  }
}
