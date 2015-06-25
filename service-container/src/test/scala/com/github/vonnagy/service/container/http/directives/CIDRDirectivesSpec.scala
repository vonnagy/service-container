package com.github.vonnagy.service.container.http.directives

import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import spray.http.HttpHeaders.`Remote-Address`
import spray.http._
import spray.testkit.Specs2RouteTest

/**
 * Created by Ivan von Nagy on 1/20/15.
 */
class CIDRDirectivesSpec extends Specification with CIDRDirectives with Specs2RouteTest with AfterAll {

  val yeah = complete("Yeah!")

  "CIDRDirectives" should {

    "allow call when no allows or denies" in {
      Get() ~> `Remote-Address`(RemoteAddress("192.168.1.1")) ~> {
        cidrFilter(Seq(), Seq()) {
          yeah
        }
      } ~> check {
        handled === true
        status === StatusCodes.OK
      }
    }

    "allow call when no denies, but matches allow" in {
      Get() ~> `Remote-Address`(RemoteAddress("192.168.1.1")) ~> {
        cidrFilter(Seq("192.168.1.1/1"), Seq()) {
          yeah
        }
      } ~> check {
        handled === true
        status === StatusCodes.OK
      }
    }

    "allow call when does not match deny, but matches allow" in {
      Get() ~> `Remote-Address`(RemoteAddress("192.168.1.1")) ~> {
        cidrFilter(Seq("192.168.1.1/1"), Seq("10.0.0.1/1")) {
          yeah
        }
      } ~> check {
        handled === true
        status === StatusCodes.OK
      }
    }

    "disallow call when no denies and does not match allow" in {
      Get() ~> `Remote-Address`(RemoteAddress("192.168.1.1")) ~> {
        cidrFilter(Seq("127.0.0.1/1"), Seq()) {
          yeah
        }
      } ~> check {
        handled === true
        status === StatusCodes.NotFound
      }
    }

    "disallow call when matches a deny" in {
      Get() ~> `Remote-Address`(RemoteAddress("10.0.0.1")) ~> {
        cidrFilter(Seq("192.168.1.1/1"), Seq("10.0.0.1/1")) {
          yeah
        }
      } ~> check {
        handled === true
        status === StatusCodes.NotFound
      }
    }

    "disallow because there is no remote address header that has been injected" in {
      Get() ~> {
        cidrFilter(Seq(), Seq()) {
          yeah
        }
      } ~> check {
        handled === true
        status === StatusCodes.NotFound
      }
    }
  }

  def afterAll = {
    if (!system.isTerminated) {
      system.shutdown
      system.awaitTermination
    }
  }
}
