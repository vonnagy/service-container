package com.github.vonnagy.service.container.metrics

import akka.actor.ActorSystem
import akka.http.scaladsl.model.MediaTypes
import akka.japi.Util._
import com.github.vonnagy.service.container.http.directives.CIDRDirectives
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints

import scala.concurrent.ExecutionContext

class MetricsEndpoints(implicit system: ActorSystem,
                       executor: ExecutionContext)
  extends RoutedEndpoints()(system, executor) with CIDRDirectives {

  lazy val writer = new MetricsWriter(Metrics(system).metricRegistry)
  lazy val config = system.settings.config.getConfig("container.http")

  val route =
    path("metrics") {
      cidrFilter(immutableSeq(config.getStringList("cidr.allow")), immutableSeq(config.getStringList("cidr.deny"))) {
        get {
          acceptableMediaTypes(MediaTypes.`application/json`) {
            encodeResponse {
              complete(writer.getMetrics(true))
            }
          }
        }
      }
    }
}
