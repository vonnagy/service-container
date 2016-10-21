package com.github.vonnagy.service.container.http.security

import akka.actor.Actor
import akka.http.scaladsl.{ConnectionContext, Http}
import com.github.vonnagy.service.container.security.SSLServerContextProvider

/**
  * This trait is applied to HttpService and is used to bring both
  */
private[http] trait SSLProvider extends SSLServerContextProvider {
  this: Actor =>

  implicit def system = context.system

  def configNamespace = "container.ssl"

  def getContext(ssl: Boolean): ConnectionContext = ssl match {
    case true =>
      isClient match {
        case true => Http().createClientHttpsContext(this.sslConfig)
        case false =>
          ConnectionContext.https(sslContext,
            sslConfig.config.enabledCipherSuites,
            sslConfig.config.enabledProtocols,
            None,
            None)
      }

    case false => ConnectionContext.noEncryption()
  }
}
