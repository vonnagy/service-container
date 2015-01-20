package com.github.vonnagy.service.container.http.routing

import akka.actor.{Actor, Props, ReceiveTimeout}
import akka.testkit.TestProbe
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import com.github.vonnagy.service.container.http.DefaultMarshallers
import org.specs2.mutable.SpecificationLike
import spray.http._
import spray.routing.RequestContext

class PerRequestSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  sequential
  implicit val marsh = DefaultMarshallers.plainMarshaller

  object Creator extends PerRequestCreator

  val probe = TestProbe()
  val req = RequestContext(HttpRequest(HttpMethods.GET), probe.ref, null)

  "The PerRequest functionality" should {

    "be able to create a request actor using Props" in {
      // Send the request
      Creator.perRequest[String](req, Props(new RequestActor), TestRequest)
      val resp = probe.receiveN(1)
      resp.head must be equalTo HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "data"), Nil, HttpProtocols.`HTTP/1.1`)
    }

    "be able to create a request actor using ActorRef" in {
      // Send the request
      Creator.perRequest[String](req, system.actorOf(Props(new RequestActor)), TestRequest)
      val resp = probe.receiveN(1)
      resp.head must be equalTo HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "data"), Nil, HttpProtocols.`HTTP/1.1`)
    }

    "be able to handle Validation message from the handler" in {
      // Send the request
      Creator.perRequest[String](req, Props(new RequestActor), TestValidation)
      val resp = probe.receiveN(1)
      resp.head must be equalTo HttpResponse(StatusCodes.BadRequest, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "bad data"), Nil, HttpProtocols.`HTTP/1.1`)
    }

    "be able to handle Error message from the handler" in {
      // Send the request
      Creator.perRequest[String](req, Props(new RequestActor), TestError)
      val resp = probe.receiveN(1)
      resp.head must be equalTo HttpResponse(StatusCodes.InternalServerError, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "bang"), Nil, HttpProtocols.`HTTP/1.1`)
    }

    "be able to handle timeout message from the handler" in {
      // Send the request
      Creator.perRequest[String](req, Props(new RequestActor), TestTimeout)
      val resp = probe.receiveN(1)
      resp.head must be equalTo HttpResponse(StatusCodes.GatewayTimeout, HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Request timeout"), Nil, HttpProtocols.`HTTP/1.1`)
    }

  }

  case object TestRequest extends RestRequest

  case object TestValidation extends RestRequest

  case object TestError extends RestRequest

  case object TestTimeout extends RestRequest

  class RequestActor extends Actor {
    def receive = {
      case TestRequest => sender ! RestResponse("data", StatusCodes.OK)
      case TestValidation => sender ! Validation("bad data", StatusCodes.BadRequest)
      case TestError => sender ! Error("bang")
      case TestTimeout => sender ! ReceiveTimeout
    }
  }

}

