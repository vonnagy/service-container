package com.github.vonnagy.service.container.http.routing

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, OneForOneStrategy}
import spray.http.StatusCode

/**
 * This class is designed for the handler of a request to extend this.
 * It provides support for the per request handling.
 */
abstract class PerRequestHandler extends Actor {

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _ => Escalate
    }
  implicit val sys = context.system

  /**
   * Send the response back
   *
   * @param message The data to send back
   * @param code The status code to use
   */
  def response(message: AnyRef, code: StatusCode): Unit = {
    context.parent ! RestResponse(message, code)
  }
}
