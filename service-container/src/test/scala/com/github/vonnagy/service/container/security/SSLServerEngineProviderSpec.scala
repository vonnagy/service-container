package com.github.vonnagy.service.container.security

import akka.actor.ActorSystem
import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.SpecificationLike

object SSLServerEngineProviderSpec {

  val conf = ConfigFactory.parseString(
    s"""
    container.http.ssl {
      |enabled=on
      |key-store-password="changeme"
      |key-password="changeme"
      |key-store = "${getClass.getClassLoader.getResource("keystore").getPath}"
      |trust-store = "${getClass.getClassLoader.getResource("truststore").getPath}"
      |trust-store-password="changeme"
      |protocol = "TLSv1"
      |random-number-generator = "AES128CounterSecureRNG"
      |enabled-algorithms = [TLS_RSA_WITH_AES_128_CBC_SHA]
    }
    """.stripMargin).withFallback(ConfigFactory.load())
}

class SSLServerEngineProviderSpec extends AkkaTestkitSpecs2Support(ActorSystem("test", SSLServerEngineProviderSpec.conf)) with SpecificationLike {

  "The SSL engine provider" should {

    "be able to create a SSL client engine" in {
      val sys = system

      val prov = new SSLClientEngineProvider {
        implicit def system = sys
        def configNamespace = "container.http.ssl"
      }

      val engine = prov.engine
      engine must not beNull
      val client = engine.getUseClientMode
      client must be equalTo (true)
    }

    "be able to create a SSL server engine" in {
      val sys = system

      val prov = new SSLServerEngineProvider {
        implicit def system = sys
        def configNamespace = "container.http.ssl"
      }

      val engine = prov.engine
      engine must not beNull
      val client = engine.getUseClientMode
      client must be equalTo (false)
    }
  }

}
