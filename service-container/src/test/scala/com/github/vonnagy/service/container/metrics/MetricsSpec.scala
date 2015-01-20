package com.github.vonnagy.service.container.metrics

import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import org.specs2.mutable.SpecificationLike

class MetricsSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  val reg = Metrics().metricRegistry

  "Metrics" should {

    "allow for counters" in {
      val name = "test.counter"
      val m = Counter(name)
      m.name must be equalTo name

      m.incr
      m.incr(10)
      m.incr(-1)
      m + 1
      m - 1
      reg.getCounters.containsKey(name) must be equalTo true
      reg.getCounters.get(name).getCount must be equalTo 10
      m.value must be equalTo 10
    }

    "allow for gauges" in {
      val name = "test.gauge"
      val m = Gauge[Int](name) {
        10
      }
      m.name must be equalTo name

      reg.getGauges.containsKey(name) must be equalTo true
      reg.getGauges.get(name).asInstanceOf[com.codahale.metrics.Gauge[Int]].getValue must be equalTo 10
      m.value must be equalTo 10
    }

    "allow for timers" in {
      val name = "test.timer"
      val m = Timer(name)
      m.name must be equalTo name

      m.time {
        "test "
      }
      m.record(10)
      reg.getTimers.containsKey(name) must be equalTo true
      reg.getTimers.get(name).getCount must be equalTo 2
      m.count must be equalTo 2
    }

    "allow for histograms" in {
      val name = "test.histogram"
      val m = Histogram(name)
      m.name must be equalTo name

      m.update(11)
      m.update(-1)
      reg.getHistograms.containsKey(name) must be equalTo true
      reg.getHistograms.get(name).getCount must be equalTo 2
      m.count must be equalTo 2
    }

    "allow for meters" in {
      val name = "test.meter"
      val m = Meter(name)
      m.name must be equalTo name

      m.mark
      m.mark(10)
      m.mark(-1)

      m.meter {
        "test"
      }

      reg.getMeters.containsKey(name) must be equalTo true
      reg.getMeters.get(name).getCount must be equalTo 11
      m.count must be equalTo 11
    }
  }

}
