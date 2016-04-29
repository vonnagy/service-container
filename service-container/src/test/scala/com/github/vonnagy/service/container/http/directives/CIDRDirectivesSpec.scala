package com.github.vonnagy.service.container.http.directives

import java.net.InetAddress

import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{RemoteAddress, StatusCodes}
import com.github.vonnagy.service.container.Specs2RouteTest
import com.github.vonnagy.service.container.http.routing.Rejection.NotFoundRejection
import org.specs2.mutable.Specification

/**
  * Created by Ivan von Nagy on 1/20/15.
  */
class CIDRDirectivesSpec extends Specification with CIDRDirectives with Specs2RouteTest {

  val yeah = complete("Yeah!")

  def remoteAddress(ip: String) = RemoteAddress(InetAddress.getByName(ip))

  "CIDRDirectives" should {

    "allow call when no allows or denies" in {
      Get() ~> addHeaders(`Remote-Address`(remoteAddress("192.168.1.1"))) ~> {
        cidrFilter(Seq(), Seq()) {
          yeah
        }
      } ~> check {
        handled === true
        status === StatusCodes.OK
      }
    }

    "allow call when no denies, but matches allow" in {
      Get() ~> addHeaders(`Remote-Address`(remoteAddress("192.168.1.1"))) ~> {
        cidrFilter(Seq("192.168.1.1/1"), Seq()) {
          yeah
        }
      } ~> check {
        handled === true
        status === StatusCodes.OK
      }
    }

    "allow call when does not match deny, but matches allow" in {
      Get() ~> addHeaders(`Remote-Address`(remoteAddress("192.168.1.1"))) ~> {
        cidrFilter(Seq("192.168.1.1/1"), Seq("10.0.0.1/1")) {
          yeah
        }
      } ~> check {
        handled === true
        status === StatusCodes.OK
      }
    }

    "disallow call when no denies and does not match allow" in {
      Get() ~> addHeaders(`Remote-Address`(remoteAddress("192.168.1.1"))) ~> {
        cidrFilter(Seq("127.0.0.1/1"), Seq()) {
          yeah
        }
      } ~> check {
        handled must beFalse
        rejections.size must beEqualTo(1)
        rejections.head must be equalTo(NotFoundRejection("The requested resource could not be found"))
      }
    }

    "disallow call when matches a deny" in {
      Get() ~> addHeaders(`Remote-Address`(remoteAddress("10.0.0.1"))) ~> {
        cidrFilter(Seq("192.168.1.1/1"), Seq("10.0.0.1/1")) {
          yeah
        }
      } ~> check {
        handled must beFalse
        rejections.size must beEqualTo(1)
        rejections.head must be equalTo(NotFoundRejection("The requested resource could not be found"))
      }
    }

    "disallow because there is no remote address header that has been injected" in {
      Get() ~> {
        cidrFilter(Seq(), Seq()) {
          yeah
        }
      } ~> check {
        handled must beFalse
        rejections.size must beEqualTo(1)
        rejections.head must be equalTo(NotFoundRejection("The requested resource could not be found"))
      }
    }
  }
}
