# Metrics

## Overview

The service container allows metrics to be gathered and reported on. Simple interfaces are provided such the the user of
the framework can easily begin to track metrics. Metrics reporting is another part of the system and allows for the use of
pre-canned reporters or to create new ones. 
 
The system provides the following metric types:

* Counter - A counter is a simple incrementing and decrementing 64-bit integer.
* Gauge - A gauge simply measures a value at a given point of time.
* Histogram - A histogram measures the distribution of values in a stream of data.
* Meter - A meter measures the *rate* at which a particular event occurs.
* Timer -A timer is the combination of a `histogram` and a `meter`.

Metrics names should use *dot* notation to differentiate the metrics. An example of a metric for the time to process a request for the endpoint `/metrics` might look like the following: **http.metrics.get**

## Gathering Metrics
Gathering metrics within the system is simple. You simple create an intance of a metric and provide an _implicit_ `ActorSystem`.

```scala
val myCounter = Counter("db.product.get")
val myHistogram = Histogram("db.product.event")
val myMeter = Meter("db.product.rate")
val myGauge = Gauge("queue.waiting") {
    myQueue.size
}
val myTime = Time("db.product.time")

// Update the metrics
myCounter.incr
myHistogram.update(10)
myMeter.mark
myTime.time {
    // Do something
}
```

## Reporting on Metrics
Once metrics are gathered they can be reported to other sources. Reporters are defined in the configuration files and follow a standard pattern. They are defined under `container.metrics.reporters` and have there own named sections. The base Service Container library contains a default reported which reports to the log file. It looks like the following in the default configuration.

```hocon
container {
    metrics {
        reporters {
          Slf4j {
            # The name of the reporter class
            class = "com.github.vonnagy.service.container.metrics.reporting.Slf4jReporter"
            # Is the reporter enabled
            enabled = on
            # What is the interval to report on
            reporting-interval = 60s
            # What is the logger
            logger = "com.github.vonnagy.service.container.metrics"
          }
        }
    }
}
```

The following fields are **required** for every reporter:
* class - The *fqdn* for the reporter class
* enabled - If the reporter is enabled then it will be loaded
* reporting-interval - Each reporter can define it's own reporting interval

The Service Container also provides a library with extra reporters: `DogStatsDReporter` and `StatsDReporter`. They are container in the `service-container-metrics-reporting`.

### Custom Reporters
It is simple to create your own reporter by deriving your own reporter class from `com.github.vonnagy.service.container.metrics.reporting.ScheduledReporter`. 
You will also provide it's own configuration (see above). Below is an example of creating a [Datadog](https://www.datadoghq.com/) reporter which will report metrics to a Datadog account. 
It depends on the metrics Datadog library (https://github.com/coursera/metrics-datadog):

`"org.coursera" % "metrics-datadog" % "1.1.1"`

```scala
package mypackage.DatadogHttpReporter

import java.util.concurrent.TimeUnit
import com.github.vonnagy.service.container.log.LoggingAdapter
import com.github.vonnagy.service.container.metrics.reporting.ScheduledReporter
import akka.actor.ActorSystem;
import com.typesafe.config.Config;

class DatadogHttpReporter(implicit val system: ActorSystem, val config: Config) extends ScheduledReporter with LoggingAdapter {

  private lazy val reporter = getReporter
  private val prefix = config.getString("metric-prefix")

  /**
   * Stop the scheduled metric reporting
   */
  override def stop: Unit = {
    log.info("Stopping the UDP metrics reporter");
    super.stop
    if (reporter != null)
      reporter.stop
  }

  /**
   * This is the method that gets called so that the metrics
   * reporting can occur.
   */
  def report(): Unit = {
    reporter.report(metrics.metricRegistry.getGauges(),
      metrics.metricRegistry.getCounters(),
      metrics.metricRegistry.getHistograms(),
      metrics.metricRegistry.getMeters(),
      metrics.metricRegistry.getTimers())
  }

  private def getReporter: DatadogReporter = {
    log.info("Initializing the DatadogHttp metrics reporter");
    val tags = Seq(
      s"app:${application.replace(" ", "-").toLowerCase}",
      s"version:$version")

    DatadogReporter.forRegistry(metrics.metricRegistry)
      .withHost(host)
      .withTransport(new HttpTransport.Builder().withApiKey(apiKey).build())
      .withTags(tags)
      .withExpansions(DatadogReporter.Expansion.ALL).build()
  }
}
```

This is the config for the example above:
```hocon
container {
    metrics {
        reporters {
            datadog {
              # The name of the reporter class
              class = "mypackage.DatadogHttpReporter"
              # Is the reporter enabled
              enabled = on
              # What is the interval to report on
              reporting-interval = 10s
              # API key
              api-key = ""
            }
        }
    }
}
```



