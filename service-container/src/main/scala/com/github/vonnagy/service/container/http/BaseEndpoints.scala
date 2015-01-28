package com.github.vonnagy.service.container.http

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.japi.Util._
import com.github.vonnagy.service.container.http.directives.CIDRDirectives
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import org.joda.time.{DateTime, DateTimeZone}
import spray.http.StatusCodes


class BaseEndpoints(implicit system: ActorSystem,
                    actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints with CIDRDirectives {

  lazy val config = system.settings.config.getConfig("container.http")

  val route = {
    path("favicon.ico") {
      complete(StatusCodes.NoContent)
    } ~
    path("ping") {
      respondPlain {
        complete("pong: ".concat(new DateTime(System.currentTimeMillis(), DateTimeZone.UTC).toString))
      }
    } ~
    path("shutdown") {
      post {
        cidrFilter(immutableSeq(config.getStringList("cidr.allow")), immutableSeq(config.getStringList("cidr.deny"))) {
          respondPlain { ctx =>
            ctx.complete("The system is being shutdown: ".concat(new DateTime(System.currentTimeMillis(), DateTimeZone.UTC).toString))
            sys.exit(0)
          }
        }
      }
    }
  }

}

