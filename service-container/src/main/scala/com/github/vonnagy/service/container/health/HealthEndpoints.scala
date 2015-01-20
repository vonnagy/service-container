package com.github.vonnagy.service.container.health

import akka.actor.{ActorRefFactory, ActorSystem, Props}
import com.github.vonnagy.service.container.http.DefaultMarshallers
import com.github.vonnagy.service.container.http.directives.CIDRDirectives
import com.github.vonnagy.service.container.http.routing.{PerRequestHandler, RestRequest, RoutedEndpoints}
import org.joda.time.DateTime
import spray.http.MediaTypes
import spray.http.StatusCodes._
import akka.japi.Util.immutableSeq
import scala.util.{Failure, Success}

// Message class for requesting the system's health
case class HealthRequest(loadBalancer: Boolean) extends RestRequest

/**
 * The REST endpoints for checking the system's health
 */
class HealthEndpoints(implicit system: ActorSystem,
                      actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints with CIDRDirectives {

  lazy val config = system.settings.config.getConfig("container.http")

  val route = {
    pathPrefix("health") {
      cidrFilter(immutableSeq(config.getStringList("cidr.allow")), immutableSeq(config.getStringList("cidr.deny"))) {
        get {
          pathEnd {
            acceptableMediaTypes(MediaTypes.`application/json`) {
              compressResponseIfRequested() {
                respondJson {
                  ctx =>
                    implicit def marsh = DefaultMarshallers.jsonMarshaller
                    perRequest[ContainerHealth](ctx, HealthHandler.props(), HealthRequest(false))
                }
              }
            }
          } ~
            path("lb") {
              respondPlain {
                ctx =>
                  implicit def marsh = DefaultMarshallers.plainMarshaller
                  perRequest[String](ctx, HealthHandler.props(), HealthRequest(true))
              }
            }
        }
      }
    }
  }

}


object HealthHandler {
  def props(): Props = Props[HealthHandler]
}

/**
 * The handler that is instantiated when the system's health is requested
 */
class HealthHandler extends PerRequestHandler with HealthProvider {

  implicit val system = context.system
  implicit val executor = context.dispatcher

  def receive = {
    case HealthRequest(loadBalancer) => runChecks.onComplete {
      case Success(check) =>
        check.state match {
          case HealthState.OK => response(serialize(loadBalancer, check), OK)
          case HealthState.DEGRADED => response(serialize(loadBalancer, check), OK)
          case HealthState.CRITICAL => response(serialize(loadBalancer, check), ServiceUnavailable)
        }

      case Failure(t) =>
        log.error("An error occurred while fetching the system's health", t)
        response(ContainerHealth(ContainerInfo.host, ContainerInfo.application, ContainerInfo.applicationVersion, ContainerInfo.containerVersion, DateTime.now, HealthState.CRITICAL, t.getMessage, Nil), InternalServerError)
    }
  }
}
