package com.github.vonnagy.service.container.health

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.japi.Util.immutableSeq
import com.github.vonnagy.service.container.http.directives.CIDRDirectives
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import org.joda.time.DateTime
import spray.http.MediaTypes
import spray.http.StatusCodes._
import spray.httpx.marshalling.{Marshaller, ToResponseMarshaller}
import spray.routing.Route

import scala.util.{Failure, Success}

/**
 * The REST endpoints for checking the system's health
 */
class HealthEndpoints(implicit val system: ActorSystem,
                      actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints with HealthProvider with CIDRDirectives {

  lazy val config = system.settings.config.getConfig("container.http")
  implicit val executor = system.dispatcher

  val route = {
    pathPrefix("health") {
      cidrFilter(immutableSeq(config.getStringList("cidr.allow")), immutableSeq(config.getStringList("cidr.deny"))) {
        get {
          pathEnd {
            acceptableMediaTypes(MediaTypes.`application/json`) {
              compressResponseIfRequested() {
                respondJson {
                  handleHealth(false)(jsonMarshaller)
                }
              }
            }
          } ~
            path("lb") {
              handleHealth(true)(plainMarshaller)
            }

        }
      }
    }
  }


  /**
   * The handler that is instantiated when the system's health is requested
   */
  def handleHealth(loadBalancer: Boolean)(implicit marshaller: Marshaller[AnyRef]): Route = ctx =>

    runChecks.onComplete {
      case Success(check) =>
        check.state match {
          case HealthState.OK => ctx.complete(serialize(loadBalancer, check))(marshaller)
          case HealthState.DEGRADED => ctx.complete(serialize(loadBalancer, check))(marshaller)
          case HealthState.CRITICAL =>
            ctx.complete(serialize(loadBalancer, check))(ToResponseMarshaller.fromMarshaller(ServiceUnavailable)(marshaller))
        }

      case Failure(t) =>
        log.error("An error occurred while fetching the system's health", t)
        ctx.complete(InternalServerError, ContainerHealth(ContainerInfo.host, ContainerInfo.application,
          ContainerInfo.applicationVersion, ContainerInfo.containerVersion,
          DateTime.now, HealthState.CRITICAL, t.getMessage, Nil))(marshaller)
    }
}
