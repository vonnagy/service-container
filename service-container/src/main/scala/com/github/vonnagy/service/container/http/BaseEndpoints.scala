package com.github.vonnagy.service.container.http

import akka.actor.{ActorRefFactory, ActorSystem}
import com.github.vonnagy.service.container.http.directives.CIDRDirectives
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import org.joda.time.{DateTime, DateTimeZone}
import spray.http.StatusCodes


class BaseEndpoints(implicit system: ActorSystem,
                    actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints with CIDRDirectives {

  implicit val config = system.settings.config.getConfig("container.http")

  val route = {
    get {
      path("favicon.ico") {
        complete(StatusCodes.NoContent)
      } ~
        path("ping") {
          respondPlain {
            complete("pong: ".concat(new DateTime(System.currentTimeMillis(), DateTimeZone.UTC).toString))
          }
        }
    } ~
      post {
        path("shutdown") {
          cidrFilter {
            respondPlain { ctx =>
              ctx.complete("The system is being shutdown: ".concat(new DateTime(System.currentTimeMillis(), DateTimeZone.UTC).toString))
              sys.exit(0)
            }
          }
        }
      }
  }

}

