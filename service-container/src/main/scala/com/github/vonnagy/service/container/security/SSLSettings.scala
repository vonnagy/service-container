package com.github.vonnagy.service.container.security

import java.io.File

import com.github.vonnagy.service.container.log.LoggingAdapter
import com.typesafe.config.{Config, ConfigException}

case class SSLSettings(
                        enabled: Boolean = false,
                        keyStore: Option[File] = None,
                        keyStorePassword: Option[String] = None,
                        keyPassword: Option[String] = None,
                        trustStore: Option[File] = None,
                        trustStorePassword: Option[String] = None,
                        protocol: Option[String] = None,
                        enabledAlgorithms: Set[String] = Set.empty,
                        randomNumberGenerator: Option[String] = None) {

}

object SSLSettings extends LoggingAdapter {

  /**
    * Parse the SSL settings based on the passed config and the optional namespace.
    *
    * @param conf      a config instance that is used to parse or locate settings within a specific namespace
    * @param namespace an optional namespace
    * @return [[com.github.vonnagy.service.container.security.SSLSettings]] object
    */
  def parse(conf: Config, namespace: Option[String] = None): SSLSettings = {
    import scala.collection.JavaConversions._

    val space = if (namespace.isDefined && !namespace.get.isEmpty) s"${namespace.get}." else ""

    val enabled = if (conf.hasPath(s"${space}enabled")) conf.getBoolean(s"${space}enabled") else false

    if (enabled) {
      val keyStore = if (conf.hasPath(s"${space}key-store") && !conf.getString(s"${space}key-store").isEmpty) Some(new File(conf.getString(s"${space}key-store"))) else None

      val keyStorePassword = if (conf.hasPath(s"${space}key-store-password") && !conf.getString(s"${space}key-store-password").isEmpty) Some(conf.getString(s"${space}key-store-password")) else None

      val keyPassword = if (conf.hasPath(s"${space}key-password") && !conf.getString(s"${space}key-password").isEmpty) Some(conf.getString(s"${space}key-password")) else None

      val trustStore = if (conf.hasPath(s"${space}trust-store") && !conf.getString(s"${space}trust-store").isEmpty) Some(new File(conf.getString(s"${space}trust-store"))) else None

      val trustStorePassword = if (conf.hasPath(s"${space}trust-store-password") && !conf.getString(s"${space}trust-store-password").isEmpty) Some(conf.getString(s"${space}trust-store-password")) else None

      val protocol = if (conf.hasPath(s"${space}protocol") && !conf.getString(s"${space}protocol").isEmpty) Some(conf.getString(s"${space}protocol")) else None

      val enabledAlgorithms = if (conf.hasPath(s"${space}enabled-algorithms"))
        conf.getStringList(s"${space}enabled-algorithms").map(_.trim).filter(_.nonEmpty).toSet
      else Set[String]()

      val randomNumberGenerator = if (conf.hasPath(s"${space}random-number-generator") && !conf.getString(s"${space}random-number-generator").isEmpty) Some(conf.getString(s"${space}random-number-generator")) else None

      if (protocol.isEmpty) throw new ConfigException.Missing(
        s"Configuration option '${space}enabled is turned on but no protocol is defined in '${space}protocol'.")
      if (keyStore.isEmpty && trustStore.isEmpty) throw new ConfigException.Missing(
        s"Configuration option '${space}enabled is turned on but no key/trust store is defined in '${space}key-store' / '${space}trust-store'.")
      if (keyStore.isDefined && keyStorePassword.isEmpty) throw new ConfigException.Missing(
        s"Configuration option '${space}key-store' is defined but no key-store password is defined in '${space}key-store-password'.")
      if (keyStore.isDefined && keyPassword.isEmpty) throw new ConfigException.Missing(
        s"Configuration option '${space}key-store' is defined but no key password is defined in '${space}key-password'.")
      if (trustStore.isDefined && trustStorePassword.isEmpty) throw new ConfigException.Missing(
        s"Configuration option '${space}trust-store' is defined but no trust-store password is defined in '${space}trust-store-password'.")

      SSLSettings(true, keyStore, keyStorePassword, keyPassword, trustStore, trustStorePassword, protocol, enabledAlgorithms, randomNumberGenerator)

    }
    else {
      SSLSettings()
    }
  }


}