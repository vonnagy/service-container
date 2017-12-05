package com.github.vonnagy.service.container.health

import java.net.InetAddress

import akka.http.scaladsl.model.headers.{Accept, `Remote-Address`}
import akka.http.scaladsl.model.{ContentTypes, MediaTypes, RemoteAddress, StatusCodes}
import com.github.vonnagy.service.container.Specs2RouteTest
import com.github.vonnagy.service.container.http.routing.Rejection.NotFoundRejection
import org.specs2.mutable.Specification

import scala.concurrent.Future

class HealthEndpointsSpec extends Specification with Specs2RouteTest {

  sequential

  val endpoints = new HealthEndpoints()(system, system.dispatcher)
  def remoteAddress(ip: String) = RemoteAddress(InetAddress.getByName(ip))

  "The routing infrastructure" should {

    "support a call to /health" in {
      Get("/health").withHeaders(`Remote-Address`(remoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        contentType === ContentTypes.`application/json`
        status must be equalTo (StatusCodes.OK)
      }
    }

    "support a call to /health that should return json" in {
      Get("/health").withHeaders(Accept(MediaTypes.`application/json`),
        `Remote-Address`(remoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        mediaType === MediaTypes.`application/json`
        contentType === ContentTypes.`application/json`
      }
    }

    "a call to /health should return an error due to CIDR rules" in {
      Get("/health").withHeaders(Accept(MediaTypes.`application/json`),
        `Remote-Address`(remoteAddress("192.168.1.1"))) ~> endpoints.route ~> check {
        handled must beFalse
        rejections.size must beEqualTo(1)
        rejections.head must be equalTo(NotFoundRejection("The requested resource could not be found"))
      }
    }

    "support a call to /health/lb" in {
      Get("/health/lb").withHeaders(Accept(MediaTypes.`text/plain`),
        `Remote-Address`(remoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        mediaType === MediaTypes.`text/plain`
        responseAs[String].equals("UP")
      }
    }

    "support a call to health/lb that returns a status of `Ok` when a health check is marked as degraded" in {
      Health(system).addCheck(new HealthCheck {
        override def getHealth: Future[HealthInfo] = Future {
          HealthInfo("degraded", HealthState.DEGRADED, "")
        }
      })

      Get("/health/lb").withHeaders(Accept(MediaTypes.`text/plain`),
        `Remote-Address`(remoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        mediaType === MediaTypes.`text/plain`
        status === StatusCodes.OK
        responseAs[String].equals("UP")
      }
    }

    "support a call to health/lb that returns a status of `ServiceUnavailable` when a health check is marked as critical" in {
      Health(system).addCheck(new HealthCheck {
        override def getHealth: Future[HealthInfo] = Future {
          HealthInfo("critical", HealthState.CRITICAL, "")
        }
      })

      Get("/health/lb").withHeaders(Accept(MediaTypes.`text/plain`),
        `Remote-Address`(remoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        //handled must beTrue
        mediaType === MediaTypes.`text/plain`
        status === StatusCodes.ServiceUnavailable
        responseAs[String].equals("DOWN")
      }
    }

  }
}
