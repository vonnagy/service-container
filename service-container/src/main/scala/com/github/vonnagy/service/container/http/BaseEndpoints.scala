package com.github.vonnagy.service.container.http

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.japi.Util._
import com.github.vonnagy.service.container.http.directives.CIDRDirectives
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import com.github.vonnagy.service.container.service.ServicesManager.ShutdownService
import org.joda.time.{DateTime, DateTimeZone}


class BaseEndpoints(implicit system: ActorSystem,
                    actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints with CIDRDirectives {

  lazy val config = system.settings.config.getConfig("container.http")
  lazy val serviceActor = system.actorSelection("akka://server/user/service")

  implicit val marshaller = plainMarshaller
  import actorRefFactory.dispatcher

  val route = {
    path("favicon.ico") {
      complete(StatusCodes.NoContent)
    } ~
      path("ping") {
        complete("pong: ".concat(new DateTime(System.currentTimeMillis(), DateTimeZone.UTC).toString))
      } ~
      path("shutdown") {
        post {
          cidrFilter(immutableSeq(config.getStringList("cidr.allow")),
            immutableSeq(config.getStringList("cidr.deny"))) { ctx =>

            ctx.complete("The system is being shutdown: ".concat(new DateTime(System.currentTimeMillis(),
              DateTimeZone.UTC).toString)) andThen {
              case r =>
                // Send a message to the root actor of this service
                serviceActor ! ShutdownService(true)
            }
          }
        }
      }
  }
}

