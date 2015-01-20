package com.github.vonnagy.service.container.metrics

import com.codahale.metrics.MetricRegistry
import net.liftweb.json.DefaultFormats
import org.specs2.mutable.Specification

class MetricsWriterSpec extends Specification {

  val reg = new MetricRegistry()
  val writer = new MetricsWriter(reg)

  step {
    reg.counter("test.counter").inc(10)
    reg.meter("test.meter").mark(10)
    reg.timer("test.timer")
    reg.histogram("test.histogram").update(10)
    reg.register("test.gauge", new com.codahale.metrics.Gauge[Int] {
      def getValue: Int = 10
    })
    reg.counter("jvm.test").inc(20)
  }

  "The metrics writer" should {
    "create json for custom metrics" in {
      val json = writer.getMetrics(false)

      implicit val formats = DefaultFormats
      val value = json \ "system" \ "metrics" \ "test.counter"
      value.extract[Int] must be equalTo 10
    }

    "create json for custom and jvm metrics" in {
      val json = writer.getMetrics(true)

      implicit val formats = DefaultFormats
      val value = json \ "system" \ "metrics" \ "test.counter"
      value.extract[Int] must be equalTo 10

      val value2 = json \ "system" \ "jvm" \ "unkown" \ "jvm.test"
      value2.extract[Int] must be equalTo 20
    }
  }
}
