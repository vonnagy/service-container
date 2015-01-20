package com.github.vonnagy.service.container.http.routing

import spray.http.StatusCode
import spray.http.StatusCodes._

trait RestRequest

case class RestResponse(response: AnyRef, code: StatusCode = OK)

case class Error(message: String, code: StatusCode = InternalServerError)

case class Validation(message: String, code: StatusCode = BadRequest)