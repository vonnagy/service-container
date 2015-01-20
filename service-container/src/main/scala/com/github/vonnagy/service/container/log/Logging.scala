package com.github.vonnagy.service.container.log

import akka.actor.Actor
import akka.event.LogSource
import akka.event.slf4j.{Logger, SLF4JLogging}

trait ActorLoggingAdapter {
  this: Actor =>

  val logSrc = LogSource(self, context.system)

  @transient
  lazy val log = Logger(logSrc._2, logSrc._1)

}

/**
 * Use this trait in your class so that there is logging support
 */
trait LoggingAdapter extends SLF4JLogging {

}