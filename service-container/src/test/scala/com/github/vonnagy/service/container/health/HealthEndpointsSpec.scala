package com.github.vonnagy.service.container.health

import java.util.concurrent.TimeUnit

import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import spray.http.HttpHeaders.`Remote-Address`
import spray.http._
import spray.testkit.Specs2RouteTest

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class HealthEndpointsSpec extends Specification with Specs2RouteTest with AfterAll {

  sequential

  val endpoints = new HealthEndpoints()(system, system)

  "The routing infrastructure" should {

    "support a call to /health" in {
      Get("/health") ~> endpoints.route ~> check {
        handled must beTrue
      }
    }

    "support a call to /health that should return json" in {
      import spray.http.HttpHeaders.Accept
      Get("/health").withHeaders(Accept(MediaTypes.`application/json`), `Remote-Address`(RemoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        mediaType === MediaTypes.`application/json`
        contentType === ContentTypes.`application/json`
      }
    }

    "support a call to /health/lb" in {
      import spray.http.HttpHeaders.Accept
      Get("/health/lb").withHeaders(Accept(MediaTypes.`text/plain`), `Remote-Address`(RemoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        mediaType === MediaTypes.`text/plain`
        responseAs[String].equals("UP")
      }
    }

    "support a call to health/lb that returns a status of `Ok` when a health check is marked as degraded" in {
      import spray.http.HttpHeaders.Accept
      Health(system).addCheck(new HealthCheck {
        override def getHealth: Future[HealthInfo] = Future {
          HealthInfo("degraded", HealthState.DEGRADED, "")
        }
      })

      Get("/health/lb").withHeaders(Accept(MediaTypes.`text/plain`), `Remote-Address`(RemoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        handled must beTrue
        mediaType === MediaTypes.`text/plain`
        status === StatusCodes.OK
        responseAs[String].equals("UP")
      }
    }

    "support a call to health/lb that returns a status of `ServiceUnavailable` when a health check is marked as critical" in {
      import spray.http.HttpHeaders.Accept
      Health(system).addCheck(new HealthCheck {
        override def getHealth: Future[HealthInfo] = Future {
          HealthInfo("critical", HealthState.CRITICAL, "")
        }
      })

      Get("/health/lb").withHeaders(Accept(MediaTypes.`text/plain`), `Remote-Address`(RemoteAddress("127.0.0.1"))) ~> endpoints.route ~> check {
        //handled must beTrue
        mediaType === MediaTypes.`text/plain`
        status === StatusCodes.ServiceUnavailable
        responseAs[String].equals("DOWN")
      }
    }

  }

  def afterAll = {
    Await.result(system.terminate(), Duration(2, TimeUnit.SECONDS))
  }
}
