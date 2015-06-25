package com.github.vonnagy.service.container.http

import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import spray.http.HttpHeaders.Accept
import spray.http.{MediaType, ContentType, MediaTypes}
import spray.routing.{Route, UnacceptedResponseContentTypeRejection}
import spray.testkit.Specs2RouteTest

class BaseDirectivesSpec extends Specification with BaseDirectives with Specs2RouteTest with AfterAll {

  val `application/vnd.com.github.vonnagy.container.health-v1+json` = MediaType.custom(mainType = "application",
    subType = "vnd.com.github.vonnagy.container.health-v1+json",
    compressible = true)

  MediaTypes.register(`application/vnd.com.github.vonnagy.container.health-v1+json`)

  "The base directives" should {

    "allow the use of the `acceptableMediaTypes` directive" in {

      import MediaTypes._

      val route: Route =
        path("app-json") {
          acceptableMediaTypes(`application/json`, `application/vnd.com.github.vonnagy.container.health-v1+json`) {
            respondJson {
              complete("[]")
            }
          }
        } ~
          path("app-custom") {
            acceptableMediaTypes(`application/json`, `application/vnd.com.github.vonnagy.container.health-v1+json`) {
              respondWithMediaType(`application/vnd.com.github.vonnagy.container.health-v1+json`) {
                complete("[]")
              }
            }
          }

      Get("/app-json").withHeaders(Accept(`application/json`, `application/vnd.com.github.vonnagy.container.health-v1+json`)) ~> route ~> check {
        responseAs[String] === "[]"
        mediaType === MediaTypes.`application/json`
      }

      Get("/app-custom").withHeaders(Accept(`application/json`, `application/vnd.com.github.vonnagy.container.health-v1+json`)) ~> route ~> check {
        responseAs[String] === "[]"
        mediaType === `application/vnd.com.github.vonnagy.container.health-v1+json`
      }

      Get("/app-json").withHeaders(Accept(`text/plain`)) ~> route ~> check {
        rejection === UnacceptedResponseContentTypeRejection(Seq(ContentType(`application/json`),
          ContentType(`application/vnd.com.github.vonnagy.container.health-v1+json`)))
      }
    }
  }

  "allow for the use of standard response types" in {

    import MediaTypes._

    Get() ~> {
      respondJson {
        complete("[]")
      }
    } ~> check {
      mediaType === `application/json`
    }

    Get() ~> {
      respondPlain {
        complete("[]")
      }
    } ~> check {
      mediaType === `text/plain`
    }

  }

  def afterAll = {
    if (!system.isTerminated) {
      system.shutdown
      system.awaitTermination
    }
  }
}
