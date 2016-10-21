package com.github.vonnagy.service.container.http.routing

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, MalformedRequestContentRejection, MissingCookieRejection, Route}
import com.github.vonnagy.service.container.Specs2RouteTest
import com.github.vonnagy.service.container.http.routing.Rejection.DuplicateRejection
import org.specs2.mutable.Specification

/**
  * Created by vonnagy on 4/18/16.
  */
class RoutingHandlerSpec extends Specification with Specs2RouteTest with Directives {

  val handler = new RoutingHandler {
    def conf = system.settings.config
  }

  val dummyRoute =
    pathPrefix("test") {
      get {
        path("duplicate") {
          reject(DuplicateRejection("test"))
        } ~
          path("malformed") {
            reject(MalformedRequestContentRejection("test", new Exception("test")))
          } ~
          path("cookie") {
            reject(MissingCookieRejection("test"))
          }
      }
    }

  "the routing handler" should {

    "provide custom rejection handlers" in {
      implicit val rejHandler = handler.rejectionHandler
      val sr = Route.seal(dummyRoute)

      Get("/test/duplicate") ~> sr ~> check {
        status must be equalTo BadRequest
        responseAs[String] must be equalTo(
          """{"code":400,"message":"The request contains bad syntax or cannot be fulfilled.","details":"\"test\""}""")
      }

      Get("/test/malformed") ~> sr ~> check {
        status must be equalTo UnprocessableEntity
        responseAs[String] must be equalTo(
          """{"code":422,"message":"The request was well-formed but was unable to be followed due to semantic errors.","details":"\"test\""}""")
      }
    }

    "use the default rejection handler as a fallback" in {
      implicit val rejHandler = handler.rejectionHandler
      val sr = Route.seal(dummyRoute)

      Get("/test/cookie") ~> sr ~> check {
        status must be equalTo BadRequest
        responseAs[String] must be equalTo(
          """{"code":400,"message":"The request contains bad syntax or cannot be fulfilled.","details":"Request is missing required cookie 'test'"}""")
      }
    }

  }
}
