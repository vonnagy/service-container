package com.github.vonnagy.service.container.metrics

import akka.actor.{ActorRefFactory, ActorSystem}
import com.github.vonnagy.service.container.http.DefaultMarshallers
import com.github.vonnagy.service.container.http.directives.CIDRDirectives
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import spray.http.MediaTypes

class MetricsEndpoints(implicit system: ActorSystem,
                       actorRefFactory: ActorRefFactory)
  extends RoutedEndpoints with CIDRDirectives {

  implicit def marsh = DefaultMarshallers.jsonMarshaller
  implicit val config = system.settings.config.getConfig("container.http")

  lazy val writer = new MetricsWriter(Metrics(system).metricRegistry)

  val route =
    path("metrics") {
      cidrFilter {
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
