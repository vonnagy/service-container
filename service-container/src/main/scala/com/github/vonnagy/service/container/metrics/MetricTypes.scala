package com.github.vonnagy.service.container.metrics

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.codahale.metrics.{Gauge => CHGauge}

trait MetricName {
  def name: String
}

case class Counter(val name: String)(implicit val system: ActorSystem) extends MetricName with Instrumented {

  private val delegate = metricRegistry.counter(name)

  /**
   * Increment the counter by one
   */
  def incr: Unit = incr(1)

  def +(delta: Long): Counter = {
    this.incr(delta);
    this
  }

  def -(delta: Long): Counter = {
    this.incr(-delta);
    this
  }

  /**
   * Increment the counter by the given delta
   * @param delta the value to increment by
   */
  def incr(delta: Long) = delegate.inc(delta)

  /**
   * Get the counters current value
   * @return
   */
  def value: Long = delegate.getCount

}

case class Gauge[A](val name: String)(f: => A)(implicit val system: ActorSystem) extends MetricName with Instrumented {

  private def getGauge: CHGauge[A] = {
    if (metricRegistry.getGauges.containsKey(name)) {
      metricRegistry.getGauges.get(name).asInstanceOf[CHGauge[A]]
    }
    else {
      metricRegistry.register(name, new CHGauge[A] {
        def getValue: A = f
      })
    }

  }

  private val delegate = getGauge
  /**
   * Get the current value
   * @return
   */
  def value: A = delegate.getValue
}

case class Histogram(val name: String)(implicit val system: ActorSystem) extends MetricName with Instrumented {

  private val delegate = metricRegistry.histogram(name)

  /**
   * Update the current value for the histogram
   * @param value the value to set
   */
  def update(value: Long) = delegate.update(value)

  /**
   * Gets the count of observations.
   * @return the observation count
   */
  def count: Long = delegate.getCount
}

case class Meter(val name: String)(implicit val system: ActorSystem) extends MetricName with Instrumented {

  private val delegate = metricRegistry.meter(name)

  /**
   * Mark the occurrence of an event.
   */
  def mark: Unit = mark(1L)

  /**
   * Mark the occurrence of a given number of events.
   *
   * @param value the number of events
   */
  def mark(value: Long) = delegate.mark(value)

  /**
   * Meter the application of this function
   * @param   f function that is meter
   * @tparam A
   */
  def meter[A](f: => A): A = {
    try {
      f
    } finally {
      delegate.mark()
    }
  }

  /**
   * Gets the count of observations.
   * @return the observation count
   */
  def count: Long = delegate.getCount
}

case class Timer(val name: String)(implicit val system: ActorSystem) extends MetricName with Instrumented {

  private val delegate = metricRegistry.timer(name)

  /**
   * Time the function
   */
  def time[A](f: => A): A = time(this)(f)

  /**
   * Time the application of this function
   * @param   f function that is timed
   * @tparam A
   */
  def time[A](metric: Timer)(f: => A): A = {
    val ctx = delegate.time()
    try {
      f
    } finally {
      ctx.stop()
    }
  }

  /**
   * Record a timed event
   * @param time
   * @param unit the time unit, defaults to nanoseconds
   */
  def record(time: Long, unit: TimeUnit = TimeUnit.NANOSECONDS): Unit = delegate.update(time, unit)

  /**
   * Gets the count of observations.
   * @return the observation count
   */
  def count: Long = delegate.getCount
}