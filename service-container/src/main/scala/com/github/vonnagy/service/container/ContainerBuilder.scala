package com.github.vonnagy.service.container

import akka.actor.Props
import com.github.vonnagy.service.container.health.HealthCheck
import com.github.vonnagy.service.container.http.routing.RoutedEndpoints
import com.github.vonnagy.service.container.listener.ContainerLifecycleListener
import com.github.vonnagy.service.container.service.ContainerService
import com.typesafe.config.Config

import scala.collection.mutable.ListBuffer

/**
 * This is the main builder for constructing the container service
 */
class ContainerBuilder {

  private var config: Option[Config] = None
  private val routedEndpoints = ListBuffer.empty[Class[_ <: RoutedEndpoints]]
  private val healthChecks = ListBuffer.empty[HealthCheck]
  private val props = ListBuffer.empty[Tuple2[String, Props]]
  private val listeners = ListBuffer.empty[ContainerLifecycleListener]

  /**
   * Add a custom config
   * @param conf An instance of Config
   * @return the ContainerBuilder
   */
  def withConfig(conf: Config): ContainerBuilder = {
    this.config = Some(conf)
    this
  }

  /**
   * Add any REST endpoint handlers that your service requires
   * @param routes An instance of RoutedEndpoints
   * @return the ContainerBuilder
   */
  def withRoutes(routes: Class[_ <: RoutedEndpoints]*): ContainerBuilder = {
    this.routedEndpoints ++= routes
    this
  }

  /**
   * Add any health check handlers that your service requires
   * @param check An instance of a HealthCheck
   * @return the ContainerBuilder
   */
  def withHealthChecks(check: HealthCheck*): ContainerBuilder = {
    healthChecks ++= check
    this
  }

  /**
   * Add any props to create any additional actors for your service. You can also create
   * actors after building the Container by accessing the `system` property on the `Container`
   * object
   * @param prop An instance of ``Tuple2[String, Props]`` which declares the name for the actor and
   *             how the actor will be created
   * @return
   */
  def withActors(prop: Tuple2[String, Props]*): ContainerBuilder = {
    props ++= prop
    this
  }

  /**
    * Add any lifecycle listeners that your service requires
    * @param listener An instance of a HealthCheck
    * @return the ContainerBuilder
    */
  def withListeners(listener:ContainerLifecycleListener*) = {
    listeners ++= listener
    this
  }

  /**
   * Construct the system
   * @return An instance of ContainerServices
   */
  def build: ContainerService = {
    val svc = new ContainerService(routedEndpoints.toSeq,
      healthChecks.toSeq,
      props.toSeq,
      listeners,
      config) with App

    svc
  }
}


