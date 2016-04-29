/**
 * The work in this class was taken from the code in akka.remote.transport.netty.NettySSLSupport
 * under the Apache 2 license for Typesafe's Akka
 */

package com.github.vonnagy.service.container.security

import java.io.{FileInputStream, File, FileNotFoundException, IOException}
import java.security.{GeneralSecurityException, KeyStore, SecureRandom}
import javax.net.ssl._

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.remote.RemoteTransportException
import akka.remote.security.provider.AkkaProvider

import scala.util.Try

object SSLContextFactory {

  def apply(settings: SSLSettings, isClient: Boolean)(implicit system: ActorSystem): SSLContext = {

    val log = Logging(system, this.getClass)
    if (isClient) initializeClientSSL(settings, log) else initializeServerSSL(settings, log)
  }

  def initializeCustomSecureRandom(rngName: Option[String], log: LoggingAdapter): SecureRandom = {
    val rng = rngName match {
      case Some(r@("AES128CounterSecureRNG" | "AES256CounterSecureRNG" | "AES128CounterInetRNG" | "AES256CounterInetRNG")) =>
        log.debug("SSL random number generator set to: {}", r)
        SecureRandom.getInstance(r, AkkaProvider)
      case Some(s@("SHA1PRNG" | "NativePRNG")) =>
        log.debug("SSL random number generator set to: " + s)
        // SHA1PRNG needs /dev/urandom to be the source on Linux to prevent problems with /dev/random blocking
        // However, this also makes the seed source insecure as the seed is reused to avoid blocking (not a problem on FreeBSD).
        SecureRandom.getInstance(s)
      case Some(unknown) =>
        log.debug("Unknown SSLRandomNumberGenerator [{}] falling back to SecureRandom", unknown)
        new SecureRandom
      case None =>
        log.debug("SSLRandomNumberGenerator not specified, falling back to SecureRandom")
        new SecureRandom
    }
    rng.nextInt() // prevent stall on first access
    rng
  }

  def initializeClientSSL(settings: SSLSettings, log: LoggingAdapter): SSLContext = {
    log.debug("Client SSL is enabled, initialising ...")

    def constructClientContext(settings: SSLSettings, log: LoggingAdapter, trustStore: File, trustStorePassword: String, protocol: String): Option[SSLContext] =
      try {
        val rng = initializeCustomSecureRandom(settings.randomNumberGenerator, log)
        val trustManagers: Array[TrustManager] = {
          val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
          trustManagerFactory.init({
            val store = KeyStore.getInstance(KeyStore.getDefaultType)

            val fin = new FileInputStream(trustStore)
            try store.load(fin, trustStorePassword.toCharArray) finally Try(fin.close())
            store
          })
          trustManagerFactory.getTrustManagers
        }
        Option(SSLContext.getInstance(protocol)) map { ctx => ctx.init(null, trustManagers, rng); ctx }
      } catch {
        case e: FileNotFoundException => throw new RemoteTransportException("Client SSL connection could not be established because trust store could not be loaded", e)
        case e: IOException => throw new RemoteTransportException("Client SSL connection could not be established because: " + e.getMessage, e)
        case e: GeneralSecurityException => throw new RemoteTransportException("Client SSL connection could not be established because SSL context could not be constructed", e)
      }

    ((settings.trustStore, settings.trustStorePassword, settings.protocol) match {
      case (Some(trustStore), Some(password), Some(protocol)) => constructClientContext(settings, log, trustStore, password, protocol)
      case (trustStore, password, protocol) => throw new GeneralSecurityException(
        "One or several SSL trust store settings are missing: [trust-store: %s] [trust-store-password: %s] [protocol: %s]".format(
          trustStore,
          password,
          protocol))
    }) match {
      case Some(context) =>
        log.debug("Using client SSL context.")
        context
      case None =>
        throw new GeneralSecurityException(
          """Failed to initialize client SSL because SSL context could not be found." +
              "Make sure your settings are correct: [trust-store: %s] [trust-store-password: %s] [protocol: %s]""".format(
              settings.trustStore,
              settings.trustStorePassword,
              settings.protocol))
    }
  }

  def initializeServerSSL(settings: SSLSettings, log: LoggingAdapter): SSLContext = {
    log.debug("Server SSL is enabled, initialising ...")

    def constructServerContext(settings: SSLSettings, log: LoggingAdapter, keyStore: File, keyStorePassword: String, keyPassword: String, protocol: String): Option[SSLContext] =
      try {
        val rng = initializeCustomSecureRandom(settings.randomNumberGenerator, log)
        val factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
        factory.init({
          val store = KeyStore.getInstance(KeyStore.getDefaultType)
          val fin = new FileInputStream(keyStore)
          try store.load(fin, keyStorePassword.toCharArray) finally Try(fin.close())
          store
        }, keyPassword.toCharArray)

        val trustManagers: Option[Array[TrustManager]] = settings.trustStore map {
          trust =>
            val pwd = settings.trustStorePassword.map(_.toCharArray).orNull
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
            trustManagerFactory.init({
              val store = KeyStore.getInstance(KeyStore.getDefaultType)
              val fin = new FileInputStream(trust)
              try store.load(fin, pwd) finally Try(fin.close())
              store
            })
            trustManagerFactory.getTrustManagers
        }
        Option(SSLContext.getInstance(protocol)) map { ctx => ctx.init(factory.getKeyManagers, trustManagers.orNull, rng); ctx }
      } catch {
        case e: FileNotFoundException => throw new RemoteTransportException("Server SSL connection could not be established because key store could not be loaded", e)
        case e: IOException => throw new RemoteTransportException("Server SSL connection could not be established because: " + e.getMessage, e)
        case e: GeneralSecurityException => throw new RemoteTransportException("Server SSL connection could not be established because SSL context could not be constructed", e)
      }

    ((settings.keyStore, settings.keyStorePassword, settings.keyPassword, settings.protocol) match {
      case (Some(keyStore), Some(storePassword), Some(keyPassword), Some(protocol)) => constructServerContext(settings, log, keyStore, storePassword, keyPassword, protocol)
      case (keyStore, storePassword, keyPassword, protocol) => throw new GeneralSecurityException(
        s"SSL key store settings went missing. [key-store: $keyStore] [key-store-password: $storePassword] [key-password: $keyPassword] [protocol: $protocol]")
    }) match {
      case Some(context) =>
        log.debug("Using server SSL context")
        log.debug("Using server SSL context")
        context
      case None => throw new GeneralSecurityException(
        """Failed to initialize server SSL because SSL context could not be found.
           Make sure your settings are correct: [key-store: %s] [key-store-password: %s] [protocol: %s]""".format(
            settings.keyStore,
            settings.keyStorePassword,
            settings.protocol))
    }
  }
}
