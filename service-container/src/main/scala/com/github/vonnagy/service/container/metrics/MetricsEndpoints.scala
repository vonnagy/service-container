package com.github.vonnagy.service.container.metrics

import akka.actor.{ActorRefFactory, ActorSystem}
import akka.japi.Util._
import com.github.vonnagy.service.container.http.directives.CIDRDirectives
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import spray.http.MediaTypes

class MetricsEndpoints(implicit system: ActorSystem,
                       actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints with CIDRDirectives {

  lazy val writer = new MetricsWriter(Metrics(system).metricRegistry)
  lazy val config = system.settings.config.getConfig("container.http")
  implicit val marshaller = jsonMarshaller

  val route =
    path("metrics") {
      cidrFilter(immutableSeq(config.getStringList("cidr.allow")), immutableSeq(config.getStringList("cidr.deny"))) {
        get {
          acceptableMediaTypes(MediaTypes.`application/json`) {
            compressResponseIfRequested() {
              respondJson {
                detach() {
                  complete(writer.getMetrics(true))
                }
              }
            }
          }
        }
      }
    }
}
