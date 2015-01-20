package com.github.vonnagy.service.container.health

import akka.actor.{Actor, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

// This message is used to fetch the health
case class GetHealth()

trait HealthCheck {

  /**
   * Fetch the health for this registered checker.
   * @return returns a future to the health information
   */
  def getHealth: Future[HealthInfo]
}

trait RegisteredHealthCheck extends HealthCheck {

  // We need an actor system in order to register the check
  implicit val system: ActorSystem
  Health(system).addCheck(this)

}

/**
 * This register the actor as a health check. When the system calls for
 * its health the internal `getHealth` method sends itself a `GetHealth`
 * message. The actor should respond back with an instance of `HealthInfo`
 */
trait RegisteredHealthCheckActor extends HealthCheck {
  this: Actor =>

  Health(context.system).addCheck(this)

  override def getHealth: Future[HealthInfo] = {
    implicit val timeout = Timeout(5 seconds)
    (self ? GetHealth).mapTo[HealthInfo]
  }
}
