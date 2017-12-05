package com.github.vonnagy.service.container

import akka.actor.ActorSystem
import com.github.vonnagy.service.container.http.routing._

import scala.concurrent.ExecutionContext


class TestEndpoints(implicit system: ActorSystem,
                    executor: ExecutionContext)
  extends RoutedEndpoints()(system, executor) {

  val route = {
    path("test") {
      complete("test")
    } ~
      path("test-per-request") {
        complete("test-complete")
      }
  }
}
