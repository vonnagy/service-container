package com.github.vonnagy.service.container.http.routing

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import com.github.vonnagy.service.container.http.DefaultMarshallers
import com.github.vonnagy.service.container.http.routing.PerRequest.{WithActorRef, WithProps}
import spray.can.server.ServerSettings
import spray.http.StatusCode
import spray.http.StatusCodes._
import spray.httpx.marshalling.Marshaller
import spray.routing.RequestContext

trait PerRequest[T] extends Actor with DefaultMarshallers {

  val r: RequestContext
  val marsh: Marshaller[T]
  val target: ActorRef
  val message: RestRequest

  // Spray settings
  val settings = ServerSettings(context.system)

  context.setReceiveTimeout(settings.timeouts.requestTimeout)
  target ! message

  def receive = {
    case res: RestResponse => complete(res.code, res.response)
    case v: Validation => complete(v.code, v.message)
    case e: Error => complete(e.code, e.message)
    case ReceiveTimeout => complete(GatewayTimeout, "Request timeout")
  }

  def complete[T <: AnyRef](status: StatusCode, obj: T) = {
    implicit def x = marsh.asInstanceOf[Marshaller[T]]
    r.complete(status, obj)
    context.stop(self)
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e => {
        complete(InternalServerError, Error(e.getMessage))
        Stop
      }
    }
}

object PerRequest {

  case class WithActorRef[T](r: RequestContext, target: ActorRef, message: RestRequest, marsh: Marshaller[T]) extends PerRequest[T]

  case class WithProps[T](r: RequestContext, props: Props, message: RestRequest, marsh: Marshaller[T]) extends PerRequest[T] {
    lazy val target = context.actorOf(props)
  }

}

trait PerRequestCreator {

  def perRequest[T](r: RequestContext, target: ActorRef, message: RestRequest)(implicit fact: ActorRefFactory, marshaller: Marshaller[T]) =
    fact.actorOf(Props(classOf[WithActorRef[T]], r, target, message, marshaller))

  def perRequest[T](r: RequestContext, props: Props, message: RestRequest)(implicit fact: ActorRefFactory, marshaller: Marshaller[T]) =
    fact.actorOf(Props(classOf[WithProps[T]], r, props, message, marshaller))
}
