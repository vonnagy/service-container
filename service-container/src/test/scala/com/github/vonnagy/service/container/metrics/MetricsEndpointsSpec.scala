package com.github.vonnagy.service.container.metrics

import java.net.InetAddress

import akka.http.scaladsl.model.headers.{Accept, `Remote-Address`}
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, RemoteAddress, StatusCodes}
import com.github.vonnagy.service.container.Specs2RouteTest
import com.github.vonnagy.service.container.http.routing.Rejection.NotFoundRejection
import org.specs2.mutable.Specification

class MetricsEndpointsSpec extends Specification with Specs2RouteTest {

  val endpoints = new MetricsEndpoints()(system, system.dispatcher)

  def remoteAddress(ip: String) = RemoteAddress(InetAddress.getByName(ip))

  "The routing infrastructure should support" should {

    "a call to /metrics to be handled" in {
      Get("/metrics").withHeaders(`Remote-Address`(remoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        contentType === ContentTypes.`application/json`
        status must be equalTo (StatusCodes.OK)
      }
    }

    "a call to /metrics should return json" in {
      Get("/metrics").withHeaders(Accept(MediaTypes.`application/json`),
        `Remote-Address`(remoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        contentType === ContentTypes.`application/json`
      }
    }

    "a call to /metrics should return an error due to CIDR rules" in {
      Get("/metrics").withHeaders(Accept(MediaTypes.`application/json`),
        `Remote-Address`(remoteAddress("192.168.1.1"))) ~> endpoints.route ~> check {
        handled must beFalse
        rejections.size must beEqualTo(1)
        rejections.head must be equalTo(NotFoundRejection("The requested resource could not be found"))
      }
    }
  }

}
