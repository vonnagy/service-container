package com.github.vonnagy.service.container.metrics

import org.specs2.mutable.Specification
import spray.http.HttpHeaders.`Remote-Address`
import spray.http.{StatusCodes, RemoteAddress, ContentTypes, MediaTypes}
import spray.testkit.Specs2RouteTest

class MetricsEndpointsSpec extends Specification with Specs2RouteTest {

  val endpoints = new MetricsEndpoints()(system, system)

  "The routing infrastructure should support" should {

    "a call to /metrics to be handled" in {
      Get("/metrics") ~> endpoints.route ~> check {
        handled must beTrue
      }
    }

    "a call to /metrics should return json" in {
      import spray.http.HttpHeaders.Accept
      Get("/metrics").withHeaders(Accept(MediaTypes.`application/json`), `Remote-Address`(RemoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        contentType === ContentTypes.`application/json`
      }
    }

    "a call to /metrics should return an error due to CIDR rules" in {
      import spray.http.HttpHeaders.Accept
      Get("/metrics").withHeaders(Accept(MediaTypes.`application/json`), `Remote-Address`(RemoteAddress("192.168.1.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        status must beEqualTo(StatusCodes.NotFound)
      }
    }
  }
}
