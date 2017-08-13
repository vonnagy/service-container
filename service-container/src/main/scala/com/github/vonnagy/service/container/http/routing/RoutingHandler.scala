package com.github.vonnagy.service.container.http.routing

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.settings.RoutingSettings
import com.github.vonnagy.service.container.http.routing.Rejection.{DuplicateRejection, NotFoundRejection}
import com.github.vonnagy.service.container.http.{DefaultMarshallers, RejectionResponse}
import com.github.vonnagy.service.container.log.LoggingAdapter
import com.typesafe.config.Config
import org.json4s.jackson.Serialization

import scala.collection.immutable
import scala.util.control.NonFatal

trait RoutingHandler extends Directives with DefaultMarshallers with LoggingAdapter {

  def conf: Config
  implicit val routeSettings = RoutingSettings(conf)
  implicit val marshaller: ToEntityMarshaller[AnyRef] = jsonMarshaller

  /**
    * Wrap all Exceptions in the [[com.github.vonnagy.service.container.http.RejectionResponse]] class then marshall as json.
    */
  implicit val exceptionHandler = ExceptionHandler {
    case IllegalRequestException(info, status) => ctx => {
      ctx.log.warning("Illegal request {}\n\t{}\n\tCompleting with '{}' response",
        ctx.request, info.formatPretty, status)

      ctx.complete(RejectionResponse(status.intValue, info.format(routeSettings.verboseErrorMessages), ""))
    }
    case NonFatal(e) => ctx => {
      ctx.log.error(e, "Error during processing of request {}", ctx.request)
      ctx.complete(InternalServerError, RejectionResponse(InternalServerError.intValue,
        InternalServerError.defaultMessage, "Something bad happened"))
    }
  }

  /**
    * Wrap all Rejections in the [[com.github.vonnagy.service.container.http.RejectionResponse]] class then marshall as json.
    */
  implicit val rejectionHandler = new RejectionHandler {
    val orig = RejectionHandler.newBuilder()
      .handle { case NotFoundRejection(errorMsg) => complete(NotFound, errorMsg) }
      .handle { case DuplicateRejection(errorMsg) => complete(BadRequest, errorMsg) }
      .handle { case MalformedRequestContentRejection(errorMsg, _) => complete(UnprocessableEntity, errorMsg) }
      .handleNotFound { complete((NotFound, "The requested resource could not be found.")) }
      .result
      .seal

    def apply(v1: immutable.Seq[Rejection]): Option[Route] = {
      val originalResult = orig(v1).getOrElse(complete(StatusCodes.InternalServerError))

      Some(mapResponse(transformExceptionRejection) {
        originalResult
      })
    }
  }

  private def transformExceptionRejection(response: HttpResponse): HttpResponse = {
    response.entity match {
      // If the entity isn't Strict (and it definitely will be), don't bother
      // converting, just throw an error, because something's weird.
      case strictEntity: HttpEntity.Strict =>
        val rej = RejectionResponse(response.status.intValue, response.status.defaultMessage,
          strictEntity.data.utf8String)

        response.withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),
          Serialization.write(rej)))

      case _ =>
        throw new Exception("Unexpected entity type")
    }
  }

}
