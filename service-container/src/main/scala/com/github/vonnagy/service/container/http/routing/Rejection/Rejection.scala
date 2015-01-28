package com.github.vonnagy.service.container.http.routing.Rejection

import spray.routing.Rejection

case class ServiceUnavailableRejection(message: AnyRef) extends Rejection