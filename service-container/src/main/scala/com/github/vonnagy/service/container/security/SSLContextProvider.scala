package com.github.vonnagy.service.container.security

import javax.net.ssl.SSLContext

import akka.actor.{ActorSystem, ExtendedActorSystem}
import com.github.vonnagy.service.container.log.LoggingAdapter
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import com.typesafe.sslconfig.akka.util.AkkaLoggerFactory
import com.typesafe.sslconfig.ssl.{ConfigSSLContextBuilder, SSLConfigFactory}

trait SSLContextProvider extends LoggingAdapter {

  // The actor system
  implicit def system: ActorSystem
  // The namespace of the SSL configuration
  implicit  def configNamespace: String
  // Is this a client or server SSL configuration
  def isClient: Boolean

  lazy val sslConfig = new AkkaSSLConfig(system.asInstanceOf[ExtendedActorSystem], {
    val containerOverrides = system.settings.config.getConfig(configNamespace)
    val akkaOverrides = system.settings.config.getConfig("akka.ssl-config")
    val defaults = system.settings.config.getConfig("ssl-config")
    SSLConfigFactory.parse(containerOverrides withFallback akkaOverrides withFallback defaults)
  })

  implicit def sslContext = if (sslConfig.config.default) {
    log.info("ssl.default is true, using the JDK's default SSLContext")
    sslConfig.validateDefaultTrustManager(sslConfig.config)
    SSLContext.getDefault
  } else {
    // break out the static methods as much as we can...
    val keyManagerFactory = sslConfig.buildKeyManagerFactory( sslConfig.config)
    val trustManagerFactory = sslConfig.buildTrustManagerFactory( sslConfig.config)
    new ConfigSSLContextBuilder(new AkkaLoggerFactory(system), sslConfig.config, keyManagerFactory, trustManagerFactory).build()
  }
}

trait SSLServerContextProvider extends SSLContextProvider {

  def isClient = false

}

trait SSLClientContextProvider extends SSLContextProvider {

  def isClient = true

}