package com.github.vonnagy.service.container.security

import java.io.File

import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import com.typesafe.config.{ConfigException, ConfigFactory}
import org.specs2.mutable.SpecificationLike

class SSLSettingsSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  "SSL settings" should {

    "handle disabled SSL configuration" in {
      val sslSettings = SSLSettings.parse(system.settings.config.getConfig("container.http.ssl"), None)
      sslSettings.keyStore must be equalTo(None)
      sslSettings.keyStorePassword must be equalTo(None)
      sslSettings.keyPassword must be equalTo(None)
      sslSettings.trustStore must be equalTo(None)
      sslSettings.trustStorePassword must be equalTo(None)
      sslSettings.protocol must be equalTo(None)
      sslSettings.enabledAlgorithms must be equalTo(Set.empty)
      sslSettings.randomNumberGenerator must be equalTo(None)
    }

    "throw an exception for an un-complete, but enabled configuration" in {
      val conf = ConfigFactory.parseString("container.http.ssl.enabled=on").withFallback(system.settings.config).getConfig("container.http.ssl")
      SSLSettings.parse(conf, None) must throwA[ConfigException.Missing]
    }

    "contain correct SSL configuration values in reference.conf" in {

      val conf = ConfigFactory.parseString(
        """
          |container.http.ssl.enabled=on
          |container.http.ssl.key-password="changeme"
          |container.http.ssl.key-store="."
          |container.http.ssl.key-store-password="changeme"
          |container.http.ssl.trust-store="."
          |container.http.ssl.trust-store-password="changeme"
        """.stripMargin).withFallback(system.settings.config)

      val sslSettings = SSLSettings.parse(conf, Some("container.http.ssl"))
      sslSettings.keyStore must be equalTo (Some(new File(".")))
      sslSettings.keyStorePassword must be equalTo(Some("changeme"))
      sslSettings.keyPassword must be equalTo(Some("changeme"))
      sslSettings.trustStore must be equalTo(Some(new File(".")))
      sslSettings.trustStorePassword must be equalTo(Some("changeme"))
      sslSettings.protocol must be equalTo(Some("TLSv1"))
      sslSettings.enabledAlgorithms must be equalTo(Set("TLS_RSA_WITH_AES_128_CBC_SHA"))
      sslSettings.randomNumberGenerator must be equalTo(None)
    }


  }
}
