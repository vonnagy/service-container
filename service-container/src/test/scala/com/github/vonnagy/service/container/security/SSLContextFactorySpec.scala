package com.github.vonnagy.service.container.security

import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.SpecificationLike

class SSLContextFactorySpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  val conf = ConfigFactory.parseString(
    s"""
       |enabled=on
       |key-store-password="changeme"
       |key-password="changeme"
       |key-store = "${getClass.getClassLoader.getResource("keystore").getPath}"
       |trust-store = "${getClass.getClassLoader.getResource("truststore").getPath}"
       |trust-store-password="changeme"
       |protocol = "TLSv1"
       |random-number-generator = "AES128CounterSecureRNG"
       |enabled-algorithms = [TLS_RSA_WITH_AES_128_CBC_SHA]
        """.stripMargin)

  "The SSL context factory" should {

    "be able to create a SSL client context" in {

      val sslSettings = SSLSettings.parse(conf, None)
      val ctx = SSLContextFactory(sslSettings, true)
      ctx must not beNull
    }

    "be able to create a SSL server context" in {

      val sslSettings = SSLSettings.parse(conf, None)
      val ctx = SSLContextFactory(sslSettings, false)
      ctx must not beNull
    }
  }
}
