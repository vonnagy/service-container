package com.github.vonnagy.service.container.http.security

import akka.actor.Actor
import akka.http.scaladsl.ConnectionContext
import com.github.vonnagy.service.container.security.SSLServerContextProvider
import com.typesafe.sslconfig.akka.AkkaSSLConfig

/**
  * This trait is applied to HttpService and is used to bring both
  */
private[http] trait SSLProvider extends SSLServerContextProvider {
  this: Actor =>

  private val sslConfig = AkkaSSLConfig(context.system)

  implicit def system = context.system

  def configNamespace = "container.http.ssl"

  implicit def httpConnectionContext: ConnectionContext = sslSettings.enabled match {
    case true =>
      ConnectionContext.https(sslContext,
        sslConfig.config.enabledCipherSuites,
        sslConfig.config.enabledProtocols,
        None,
        None)

    case false =>
      ConnectionContext.noEncryption()
  }
}
