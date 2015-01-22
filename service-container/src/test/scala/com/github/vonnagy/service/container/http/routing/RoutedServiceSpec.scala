package com.github.vonnagy.service.container.http.routing

import akka.actor.ActorDSL._
import akka.actor._
import akka.testkit.{TestActorRef, TestProbe}
import com.github.vonnagy.service.container.TestEndpoints
import com.github.vonnagy.service.container.http.RejectionResponse
import org.specs2.mutable.Specification
import spray.http._
import spray.routing._
import spray.testkit.Specs2RouteTest

class RoutedServiceSpec extends Specification with Directives with Specs2RouteTest {

  def echoComplete[T]: T => Route = { x ⇒ complete(x.toString) }

  val svcAct = actor("service")(new Act {
    become { case _ => }
  })

  val testRoute = new RoutedEndpoints {
    def route = {
      path("test") { complete("test") }
    }
  }

  val probe = new TestProbe(system)
  val httpAct = TestActorRef(Props(classOf[RoutedService], Seq(testRoute)), svcAct, "http")
  val svc = httpAct.underlyingActor.asInstanceOf[RoutedService]

  "The RoutedService" should {

    "allow for routes to be defined by a passed sequence of ``RoutedEndpoints`` instances" in {

      Get("/test") ~> svc.buildRoute(svc.routes) ~> check {
        responseAs[String] must be equalTo "test"
      }
    }

    "allow for routes to be added after the system is already loaded" in {
      // This should create the actor and register the endpoints
      val r = new RoutedEndpoints {
        def route = {
          path("test2") { complete("test2") }
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
          post { path("test3") { entity(as[TestEntity]) { echoComplete } } }
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
          get { path("test4") { ctx => throw new Exception("test") } }
        }
      }

      probe.send(httpAct, AddRoute(postRoute))
      probe.expectMsg(RouteAdded)

      Get("/test4") ~>
        svc.sealRoute(svc.buildRoute(svc.routes))(svc.exceptionHandler, svc.rejectionHandler) ~> check {
        mediaType === MediaTypes.`application/json`
        responseAs[RejectionResponse] must not beNull
      }
    }
  }


  step {
    system.shutdown
    system.awaitTermination
  }
}
