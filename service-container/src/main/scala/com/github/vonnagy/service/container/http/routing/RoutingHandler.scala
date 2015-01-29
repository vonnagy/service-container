package com.github.vonnagy.service.container.http.routing

import akka.actor.ActorRefFactory
import com.github.vonnagy.service.container.http.routing.Rejection.{DuplicateRejection, NotFoundRejection}
import com.github.vonnagy.service.container.http.{DefaultMarshallers, RejectionResponse}
import com.github.vonnagy.service.container.log.LoggingAdapter
import net.liftweb.json.Serialization
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import spray.routing._

import scala.util.control.NonFatal

trait RoutingHandler extends HttpServiceBase with DefaultMarshallers with LoggingAdapter {

  implicit val fact: ActorRefFactory

  /**
   * Wrap all Exceptions in the [[com.github.vonnagy.service.container.http.RejectionResponse]] class then marshall as json.
   */
  implicit val exceptionHandler = ExceptionHandler {
    case errors => mapHttpResponse(transformRejection) {
      (ExceptionHandler.apply {
        case NonFatal(e) => ctx => {
          log.error(e.getMessage, e)
          ctx.complete(InternalServerError, InternalServerError.defaultMessage)
        }
      } orElse ExceptionHandler.default)(errors)
    }
  }

  /**
   * Wrap all Rejections in the [[com.github.vonnagy.service.container.http.RejectionResponse]] class then marshall as json.
   */
  implicit val rejectionHandler = RejectionHandler {
    case rejections => mapHttpResponse(transformRejection) {
      // Pre-default rejections
      (RejectionHandler.apply {
        case NotFoundRejection(errorMsg) :: _ =>
          complete(NotFound, errorMsg)
        case DuplicateRejection(errorMsg) :: _ =>
          complete(BadRequest, errorMsg)
        case MalformedRequestContentRejection(errorMsg, cause) :: _ =>
          complete(UnprocessableEntity, errorMsg)
      } orElse RejectionHandler.Default orElse
        // Post-default rejections
        RejectionHandler.apply {
          case _ :: _ =>
            complete(NotFound, "The requested resource could not be found.")
        })(rejections)
    }
  }

  private def transformRejection(response: HttpResponse): HttpResponse = {
    response.withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),
      Serialization.write(RejectionResponse(response.status.intValue, response.status.defaultMessage, response.entity.asString))))
  }


}
