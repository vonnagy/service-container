package com.github.vonnagy.service.container.http

import java.util.concurrent.TimeUnit

import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import spray.http.HttpHeaders.`Remote-Address`
import spray.http.{RemoteAddress, StatusCodes}
import spray.routing.Directives
import spray.testkit.Specs2RouteTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class BaseEndpointsSpec extends Specification with Directives with Specs2RouteTest with AfterAll {

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
      Post("/shutdown").withHeaders(`Remote-Address`(RemoteAddress("192.168.1.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        status must beEqualTo(StatusCodes.NotFound)
      }
    }

  }

  def afterAll = {
    Await.result(system.terminate(), Duration(2, TimeUnit.SECONDS))
  }
}
