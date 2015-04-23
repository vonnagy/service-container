package com.github.vonnagy.service.container.http.security

import akka.actor.Actor
import com.github.vonnagy.service.container.security.SSLServerEngineProvider
import spray.io.ServerSSLEngineProvider

/**
 * This trait is applied to HttpService and is used to bring both
 */
private[http] trait SSLProvider extends SSLServerEngineProvider {
  this: Actor =>

  import context.system
  def configNamespace = "container.http.ssl"

  implicit def sslEngineProvider: ServerSSLEngineProvider = if (sslSettings.enabled) {
    ServerSSLEngineProvider { eng =>
      engine.setUseClientMode(false)
      engine.setEnabledCipherSuites(sslSettings.enabledAlgorithms.toArray)
      engine
    }
  } else null

}
