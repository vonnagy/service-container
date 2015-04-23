package com.github.vonnagy.service.container.security

import javax.net.ssl.SSLContext

import akka.actor.ActorSystem

trait SSLContextProvider {

  // The actor system
  implicit def system: ActorSystem
  // The namespace of the SSL configuration
  implicit  def configNamespace: String
  // Is this a client or server SSL configuration
  def isClient: Boolean

  val sslSettings = SSLSettings.parse(system.settings.config, Some(configNamespace))

  implicit def sslContext: SSLContext = if (sslSettings.enabled) SSLContextFactory(sslSettings, isClient) else null

}

trait SSLServerContextProvider extends SSLContextProvider {

  def isClient = false

}

trait SSLClientContextProvider extends SSLContextProvider {

  def isClient = true

}