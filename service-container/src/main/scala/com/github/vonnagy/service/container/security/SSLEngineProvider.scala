package com.github.vonnagy.service.container.security

trait SSLServerEngineProvider extends SSLServerContextProvider {

  implicit def engine = sslSettings.enabled match {
    case true =>
      val eng = sslContext.createSSLEngine()
      eng.setUseClientMode(false)
      eng.setEnabledCipherSuites(sslSettings.enabledAlgorithms.toArray)
      eng

    case false => null
  }

}

trait SSLClientEngineProvider extends SSLClientContextProvider {

  implicit def engine = sslSettings.enabled match {
    case true =>
      val eng = sslContext.createSSLEngine()
      eng.setUseClientMode(true)
      eng.setEnabledCipherSuites(sslSettings.enabledAlgorithms.toArray)
      eng

    case false => null
  }

}
