package com.github.vonnagy.service.container.http.routing

import akka.actor.ActorDSL._
import akka.actor._
import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.testkit.{TestActorRef, TestProbe}
import com.github.vonnagy.service.container.Specs2RouteTest
import com.github.vonnagy.service.container.http.{DefaultMarshallers, RejectionResponse}
import org.specs2.mutable.Specification

class RoutedServiceSpec extends Specification with Directives with Specs2RouteTest {

  val probe = new TestProbe(system)
  val httpAct = TestActorRef(Props(new Act with RoutedService with DefaultMarshallers {
    become(routeReceive)
  }), "http")
  val svc = httpAct.underlyingActor.asInstanceOf[RoutedService]

  def echoComplete[T]: T => Route = { x ⇒ complete(x.toString) }

  "The RoutedService" should {

    "allow for routes to be added after the system is already loaded" in {
      // This should create the actor and register the endpoints
      val r = new RoutedEndpoints {
        def route = {
          path("test2") {
            complete("test2")
          }
        }
      }

      probe.send(httpAct, AddRoute(r))
      probe.expectMsg(RouteAdded)

      Get("/test2") ~> svc.buildRoute(svc.routes) ~> check {
        responseAs[String] must be equalTo "test2"
      }
    }

    "respond with UnprocessableEntity for requests resulting in a MalformedFormFieldRejection" in {

      case class TestEntity(id: Int, name: String)
      implicit val unmarsh = svc.jsonUnmarshaller[TestEntity]
      implicit val rejMarsh = svc.jsonUnmarshaller[RejectionResponse]

      val postRoute = new RoutedEndpoints {
        def route = {
          post {
            path("test3") {
              entity(as[TestEntity]) {
                echoComplete
              }
            }
          }
        }
      }

      probe.send(httpAct, AddRoute(postRoute))
      probe.expectMsg(RouteAdded)

      Post("/test3", HttpEntity(MediaTypes.`application/json`, """{"id":100, "namex":"product"}""")) ~>
        handleRejections(svc.rejectionHandler)(svc.buildRoute(svc.routes)) ~> check {
        status === StatusCodes.UnprocessableEntity
        mediaType === MediaTypes.`application/json`
        responseAs[RejectionResponse] must not beNull
      }
    }

    "respond with RejectionResponse for requests that error out" in {

      implicit val rejMarsh = svc.jsonUnmarshaller[RejectionResponse]

      val postRoute = new RoutedEndpoints {
        def route = {
          get {
            path("test4") { ctx => throw new Exception("test") }
          }
        }
      }

      probe.send(httpAct, AddRoute(postRoute))
      probe.expectMsg(RouteAdded)

      Get("/test4") ~>
        Route.seal(svc.buildRoute(svc.routes))(svc.routeSettings,
          exceptionHandler = svc.exceptionHandler,
          rejectionHandler = svc.rejectionHandler) ~> check {

        mediaType === MediaTypes.`application/json`
        responseAs[RejectionResponse] must not beNull
      }
    }
  }

}
