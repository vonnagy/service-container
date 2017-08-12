package com.github.vonnagy.service.container.http

import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{MediaType, MediaTypes}
import akka.http.scaladsl.server.{ContentNegotiator, Route, UnacceptedResponseContentTypeRejection}
import com.github.vonnagy.service.container.Specs2RouteTest
import org.specs2.mutable.Specification


class BaseDirectivesSpec extends Specification with BaseDirectives with DefaultMarshallers with Specs2RouteTest {

  val `application/vnd.com.github.vonnagy.container.health-v1+json` =
    MediaType.custom("application/vnd.com.github.vonnagy.container.health-v1+json", false)

  "The base directives" should {

    "allow the use of the `acceptableMediaTypes` directive" in {

      import MediaTypes._

      implicit val marsh: ToEntityMarshaller[Seq[String]] = jsonMarshaller

      implicit val vndMarsh: ToEntityMarshaller[String] =
        Marshaller.StringMarshaller.wrap(`application/vnd.com.github.vonnagy.container.health-v1+json`)(_.toString)

      val route: Route =
        path("app-json") {
          acceptableMediaTypes(`application/json`, `application/vnd.com.github.vonnagy.container.health-v1+json`) {
            complete(Seq())
          }
        } ~
          path("app-custom") {
            acceptableMediaTypes(`application/json`, `application/vnd.com.github.vonnagy.container.health-v1+json`) {
              complete("[]")
            }
          }

      Get("/app-json")
        .withHeaders(Accept(`application/json`, `application/vnd.com.github.vonnagy.container.health-v1+json`)) ~>
        route ~> check {
        responseAs[String] === "[]"
        mediaType === MediaTypes.`application/json`
      }

      Get("/app-custom")
        .withHeaders(Accept(`application/vnd.com.github.vonnagy.container.health-v1+json`, `application/json`)) ~>
        route ~> check {
        responseAs[String] === "[]"
        mediaType === `application/vnd.com.github.vonnagy.container.health-v1+json`
      }

      Get("/app-json").withHeaders(Accept(`text/plain`)) ~> route ~> check {
        rejection === UnacceptedResponseContentTypeRejection(Set(ContentNegotiator.Alternative(`application/json`),
          ContentNegotiator.Alternative(`application/vnd.com.github.vonnagy.container.health-v1+json`)))
      }
    }
  }

  "allow for the use of json response type" in {

    implicit val marsh: ToEntityMarshaller[Seq[String]] = jsonMarshaller

    Get() ~> {
      complete(Seq())
    } ~> check {
      mediaType === `application/json`
    }
  }

  "allow for the use of plain response type" in {

    Get() ~> {
      complete("[]")
    } ~> check {
      mediaType === `text/plain`
    }

  }

}
