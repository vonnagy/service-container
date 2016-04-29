package com.github.vonnagy.service.container

import akka.actor.{ActorRefFactory, ActorSystem}
import com.github.vonnagy.service.container.http.routing._


class TestEndpoints(implicit system: ActorSystem,
                    actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints {

  val route = {
    path("test") {
      complete("test")
    } ~
      path("test-per-request") {
        complete("test-complete")
      }
  }
}
