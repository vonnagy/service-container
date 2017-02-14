package com.github.vonnagy.service.container.listener

import com.github.vonnagy.service.container.service.ContainerService

/**
  * Created by alexsilva on 1/28/17.
  */
class TestContainerLifecycleListener extends ContainerLifecycleListener {
  override def onShutdown(container: ContainerService): Unit = {
    println("Listener onShutdown called.")
  }

  override def onStartup(container: ContainerService): Unit = {
    println("Listener onStartup called.")
  }
}
