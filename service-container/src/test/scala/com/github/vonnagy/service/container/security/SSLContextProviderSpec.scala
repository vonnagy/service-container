package com.github.vonnagy.service.container.security

import com.github.vonnagy.service.container.AkkaTestkitSpecs2Support
import org.specs2.mutable.SpecificationLike

class SSLContextProviderSpec extends AkkaTestkitSpecs2Support with SpecificationLike {

  val sys = system

  "SSLContextProvider" should {

    "allow for the creation of a server context" in {
      val prov = new SSLServerContextProvider {
        implicit def system = sys
        def configNamespace = "container.ssl"
      }

      prov.sslConfig must not beNull

      prov.isClient must beFalse

      val ctx = prov.sslContext
      ctx must not beNull
    }

    "allow for the creation of a client context" in {
      val prov = new SSLClientContextProvider {
        implicit def system = sys
        def configNamespace = "container.ssl"
      }

      prov.sslConfig must not beNull

      prov.isClient must beTrue

      val ctx = prov.sslContext
      ctx must not beNull

    }
  }
}
