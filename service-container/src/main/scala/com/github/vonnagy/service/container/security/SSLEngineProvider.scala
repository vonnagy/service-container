package com.github.vonnagy.service.container.security

trait SSLServerEngineProvider extends SSLServerContextProvider {

  implicit def engine = if (sslSettings.enabled) {
    val eng = sslContext.createSSLEngine()
    eng.setUseClientMode(false)
    eng.setEnabledCipherSuites(sslSettings.enabledAlgorithms.toArray)
    eng
  } else null

}

trait SSLClientEngineProvider extends SSLClientContextProvider {

  implicit def engine = if (sslSettings.enabled) {
    val eng = sslContext.createSSLEngine()
    eng.setUseClientMode(true)
    eng.setEnabledCipherSuites(sslSettings.enabledAlgorithms.toArray)
    eng
  } else null

}
