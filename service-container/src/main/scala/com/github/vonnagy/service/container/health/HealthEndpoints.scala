package com.github.vonnagy.service.container.health

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.{MediaTypes, StatusCodes}
import akka.http.scaladsl.server._
import akka.japi.Util.immutableSeq
import com.github.vonnagy.service.container.http.DefaultMarshallers
import com.github.vonnagy.service.container.http.directives.CIDRDirectives
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import org.joda.time.DateTime

import scala.util.{Failure, Success, Try}

/**
  * The REST endpoints for checking the system's health
  */
class HealthEndpoints(implicit val system: ActorSystem,
                      actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints with HealthProvider with CIDRDirectives with DefaultMarshallers {

  lazy val config = system.settings.config.getConfig("container.http")
  implicit val executor = system.dispatcher

  val route = {
    pathPrefix("health") {
      cidrFilter(immutableSeq(config.getStringList("cidr.allow")), immutableSeq(config.getStringList("cidr.deny"))) {
        get {
          pathEnd {
            acceptableMediaTypes(MediaTypes.`application/json`) {
              encodeResponse {
                onComplete(runChecks)(handleHealth(false)(jsonMarshaller))
              }
            }
          } ~
            path("lb") {
              onComplete(runChecks)(handleHealth(true)(plainMarshaller))
            }

        }
      }
    }
  }


  /**
    * The handler that is instantiated when the system's health is requested
    */

  def handleHealth[T <: ContainerHealth](loadBalancer: Boolean)(implicit marshaller: ToEntityMarshaller[AnyRef]):
  PartialFunction[Try[ContainerHealth], Route] = {

    case Success(check) =>
      check.state match {
        case HealthState.OK => complete(serialize(loadBalancer, check))
        case HealthState.DEGRADED => complete(serialize(loadBalancer, check))
        case HealthState.CRITICAL =>
          complete(StatusCodes.ServiceUnavailable -> serialize(loadBalancer, check))
      }

    case Failure(t) =>
      log.error("An error occurred while fetching the system's health", t)
      complete(StatusCodes.InternalServerError, ContainerHealth(ContainerInfo.host, ContainerInfo.application,
        ContainerInfo.applicationVersion, ContainerInfo.containerVersion,
        DateTime.now, HealthState.CRITICAL, t.getMessage, Nil))
  }

}
