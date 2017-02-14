package com.github.vonnagy.service.container

import com.github.vonnagy.service.container.health.HealthCheck
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import com.github.vonnagy.service.container.listener.ContainerLifecycleListener
import com.github.vonnagy.service.container.service.ContainerService

/**
  * This is the main builder for constructing the container service
  */

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

/**
  * This is the main builder for constructing the container service
  */
case class ContainerBuilder(
                             endpoints: Seq[Class[_ <: RoutedEndpoints]] = Seq.empty,
                             healthChecks: Seq[HealthCheck] = Seq.empty,
                             props: Seq[(String, Props)] = Seq.empty,
                             listeners: Seq[ContainerLifecycleListener] = Seq.empty,
                             config: Config = ConfigFactory.empty,
                             system: Option[ActorSystem] = None
                           ) {

  def withConfig(conf: Config): ContainerBuilder = copy(config = conf)

  def withRoutes(routes: Class[_ <: RoutedEndpoints]*): ContainerBuilder = copy(endpoints = routes)

  def withConfigValue(name: String, value: Any): ContainerBuilder =
    copy(config = this.config.withValue(name, ConfigValueFactory.fromAnyRef(value)))

  def withHealthChecks(checks: HealthCheck*): ContainerBuilder = copy(healthChecks = checks)

  def withActors(actors: (String, Props)*): ContainerBuilder = copy(props = actors)

  def withListeners(obs: ContainerLifecycleListener*): ContainerBuilder = copy(listeners = obs)

  def withActorSystem(sys: ActorSystem): ContainerBuilder = copy(system = Some(sys))

  def build: ContainerService = {
    implicit val actorSystem = system.getOrElse(ActorSystem.create("service-container", config))
    val svc = new ContainerService(endpoints, healthChecks, props, listeners,
      Some(config)) with App
    svc
  }

  def validateConfig(paths: String*) = {
    paths.foreach { path =>
      if (!config.hasPath(path)) {
        throw new MissingConfigException(s"Missing required config property: '$path'.")
      }
    }
  }
}

class MissingConfigException(s: String) extends RuntimeException(s)


