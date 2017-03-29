package com.github.vonnagy.service.container.metrics

import com.codahale.metrics._
import org.json4s.JsonDSL._
import org.json4s.{JObject, JValue, JsonAST}

import scala.collection.JavaConverters._
import scala.collection._

class MetricsWriter(registry: MetricRegistry) {

  def getMetrics(includeJvm: Boolean): JValue = {
    if (includeJvm) {
      ("system" ->
        ("jvm" -> getVmMetrics) ~
          ("metrics" -> getCustomMetrics)
        )
    }
    else {
      ("system" -> ("metrics" -> getCustomMetrics))
    }
  }

  private def getVmMetrics: JObject = {

    if (registry != null) {
      val t = SortedMap(registry.getMetrics.asScala.filterKeys(n => n.startsWith("jvm.")).to: _*)

      if (t.isEmpty) {
        JObject(Nil)
      }
      else {
        val m = t.groupBy {
          g =>
            // Grab the actual type
            g._2.getClass.getName.replace("com.codahale.metrics.jvm.", "").replace("com.codahale.metrics.", "").split("\\$")(0) match {
              case "JmxAttributeGauge" => "buffers"
              case "GarbageCollectorMetricSet" => "garbage-collection"
              case "MemoryUsageGaugeSet" => "memory"
              case "ThreadStatesGaugeSet" => "thread-states"
              case _ => "unkown"
            }
        }

        SortedMap(m.toSeq: _*).map {
          w =>
            (w._1 ->
              w._2.map {
                s =>
                  s._1 -> processMetric(s._2)
              }.foldLeft(JObject(Nil))(_ ~ _)
              )
        }.foldLeft(JObject(Nil))(_ ~ _)
      }
    }
    else {
      JObject(Nil)
    }
  }

  private def getCustomMetrics(): JObject = {

    if (registry != null) {
      val t = SortedMap(registry.getMetrics.asScala.filterKeys(n => !n.startsWith("jvm.")).to: _*)

      if (t.isEmpty) {
        JObject(Nil)
      }
      else {
        t.map {
          w => w._1 -> processMetric(w._2)
        }.foldLeft(JObject(Nil))(_ ~ _)
      }
    }
    else {
      JObject(Nil)
    }
  }

  private def processMetric(metric: Metric): JValue = {

    val Hist = classOf[com.codahale.metrics.Histogram]
    val Count = classOf[com.codahale.metrics.Counter]
    val Mtr = classOf[com.codahale.metrics.Meter]
    val Gau = classOf[com.codahale.metrics.Gauge[_]]
    val Tim = classOf[com.codahale.metrics.Timer]

    metric.getClass match {
      case Hist =>
        processHistogram(metric.asInstanceOf[com.codahale.metrics.Histogram])
      case Count =>
        processCounter(metric.asInstanceOf[com.codahale.metrics.Counter])
      case Mtr =>
        processMeter(metric.asInstanceOf[com.codahale.metrics.Meter])
      case Gau =>
        processGauge(metric.asInstanceOf[com.codahale.metrics.Gauge[_]])
      case Tim =>
        processTimer(metric.asInstanceOf[com.codahale.metrics.Timer])
      case _ => // This is a metric set which contains functions as the metric value
        if (Hist.isInstance(metric)) {
          processHistogram(metric.asInstanceOf[com.codahale.metrics.Histogram])
        }
        else if (Count.isInstance(metric)) {
          processCounter(metric.asInstanceOf[com.codahale.metrics.Counter])
        }
        else if (Mtr.isInstance(metric)) {
          processMeter(metric.asInstanceOf[com.codahale.metrics.Meter])
        }
        else if (Gau.isInstance(metric)) {
          processGauge(metric.asInstanceOf[com.codahale.metrics.Gauge[_]])
        }
        else {
          processTimer(metric.asInstanceOf[com.codahale.metrics.Timer])
        }
    }
  }

  private def processHistogram(metric: com.codahale.metrics.Histogram): JObject = {
    ("count" -> metric.getCount) ~
      writeSampling(metric)
  }

  private def processCounter(metric: com.codahale.metrics.Counter): JValue = {
    metric.getCount
  }

  private def processGauge(metric: com.codahale.metrics.Gauge[_]): JValue = {
    evaluateGauge(metric)
  }

  private def processMeter(metric: com.codahale.metrics.Meter): JObject = {
    ("event_type" -> metric.getCount) ~
      writeMeteredFields(metric)
  }

  private def processTimer(metric: com.codahale.metrics.Timer): JObject = {
    ("duration" ->
      ("unit" -> "nanoseconds") ~
        writeSampling(metric)
      ) ~
      ("rate" -> writeMeteredFields(metric))
  }

  private def tf(map: Map[String, Any]): JObject = {
    val met = classOf[Metric]

    map.groupBy(g => g._1.split("\\.")(0)).map { w =>
      val key = w._1
      w._2.map { i =>
        val obj = i._2 match {
          case m if met.isAssignableFrom(i._2.getClass) =>
            if (i._1.contains(key.concat("."))) {
              val k = i._1.replace(key.concat("."), "")

              if (met.isAssignableFrom(i._2.getClass) && !k.contains(".")) {
                key -> tf(Map(k -> i._2.asInstanceOf[Metric]))
              }
              else {
                key -> tf(Map(k -> i._2))
              }
            }
            else {
              key -> processMetric(m.asInstanceOf[Metric])
            }
          case _ =>
            val b = i._2.asInstanceOf[Map[String, Any]].map { s =>
              s._1.replace(key.concat("."), "") -> s._2
            }
            (key -> tf(b))
        }
        obj
      }.foldLeft(JObject(Nil))(_ ~ _)
    }.reduceLeft(_ ~ _)
  }

  private def evaluateGauge(gauge: com.codahale.metrics.Gauge[_]): JValue = {
    try {
      matchAny(gauge.getValue)
    } catch {
      case e: Throwable =>
        string2jvalue("Error evaluating the gauge: " + e.getMessage)
    }
  }

  private def writeSampling(metric: Sampling): JObject = {
    val snapshot = metric.getSnapshot

    ("min" -> snapshot.getMin) ~
      ("max" -> snapshot.getMax) ~
      ("mean" -> snapshot.getMean) ~
      ("median" -> snapshot.getMedian) ~
      ("std_dev" -> snapshot.getStdDev) ~
      ("p75" -> snapshot.get75thPercentile) ~
      ("p95" -> snapshot.get95thPercentile) ~
      ("p98" -> snapshot.get98thPercentile) ~
      ("p99" -> snapshot.get99thPercentile) ~
      ("p999" -> snapshot.get999thPercentile)
  }

  private def writeMeteredFields(metric: Metered): JObject = {
    ("unit" -> "events/second") ~
      ("count" -> metric.getCount) ~
      ("mean" -> metric.getMeanRate) ~
      ("m1" -> metric.getOneMinuteRate) ~
      ("m5" -> metric.getFiveMinuteRate) ~
      ("m15" -> metric.getFifteenMinuteRate)
  }

  private def matchAny(num: Any): JValue = {
    try {
      (num: Any) match {
        case z: Boolean => boolean2jvalue(z)
        case b: Byte => int2jvalue(b.toInt)
        case c: Char => int2jvalue(c.toInt)
        case s: Short => int2jvalue(s.toInt)
        case i: Int => int2jvalue(i)
        case j: Long => long2jvalue(j)
        case f: Float => float2jvalue(f)
        case d: Double => bigdecimal2jvalue(d)
        case st: String => string2jvalue(st)
        case r: AnyRef => JsonAST.JNull
      }
    } catch {
      case e: Throwable =>
        string2jvalue("Error evaluating the value: " + e.getMessage)
    }
  }
}
