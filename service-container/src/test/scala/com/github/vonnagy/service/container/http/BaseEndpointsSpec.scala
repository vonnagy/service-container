package com.github.vonnagy.service.container.http

import java.net.InetAddress

import akka.http.scaladsl.model.headers.`Remote-Address`
import akka.http.scaladsl.model.{RemoteAddress, StatusCodes}
import akka.http.scaladsl.server.Directives
import com.github.vonnagy.service.container.Specs2RouteTest
import com.github.vonnagy.service.container.http.routing.Rejection.NotFoundRejection
import org.specs2.mutable.Specification

class BaseEndpointsSpec extends Specification with Directives with Specs2RouteTest {

  val endpoints = new BaseEndpoints

  "The base routing infrastructure" should {

    "return no content for favicon.ico" in {
      Get("/favicon.ico") ~> endpoints.route ~> check {
        status must be equalTo StatusCodes.NoContent
      }
    }

    "support a call to ping" in {
      Get("/ping") ~> endpoints.route ~> check {
        responseAs[String] must startWith("pong")
        status must beEqualTo(StatusCodes.OK)
      }
    }

    "a call to shutdown should return and error due to CIDR rules" in {
      Post("/shutdown").withHeaders(`Remote-Address`(RemoteAddress(InetAddress.getByName("192.168.1.1")))) ~> endpoints.route ~> check {
        handled must beFalse
        rejections.size must beEqualTo(1)
        rejections.head must be equalTo(NotFoundRejection("The requested resource could not be found"))
      }
    }

  }

}
