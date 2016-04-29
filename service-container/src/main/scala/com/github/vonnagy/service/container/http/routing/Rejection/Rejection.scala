package com.github.vonnagy.service.container.http.routing.Rejection

import akka.http.scaladsl.server.Rejection

/**
  * Use this rejection when a service is not available
 *
 * @param message
 */
case class ServiceUnavailableRejection(message: String) extends Rejection

/**
 * Use this rejection when trying to create an entity that already exists
 *
 * @param message
 */
case class DuplicateRejection(message: String) extends Rejection

/**
 * Use this rejection when a resource can't be found and you want a custom message
 *
 * @param message
 */
case class NotFoundRejection(message: String) extends Rejection