package com.github.vonnagy.service.container

import akka.actor.{ActorRefFactory, ActorSystem, Props}
import com.github.vonnagy.service.container.http.routing._


class TestEndpoints(implicit system: ActorSystem,
                    actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints {

  val route = {
    path("test") {
      respondPlain {
        complete("test")
      }
    } ~
      path("test-per-request") {
        respondPlain {
          complete("test-complete")
        }
      }
  }
}
