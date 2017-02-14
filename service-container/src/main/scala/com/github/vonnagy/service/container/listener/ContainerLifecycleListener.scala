package com.github.vonnagy.service.container.listener

import com.github.vonnagy.service.container.service.ContainerService

/**
  * Classes implementing this contract receive container life-cycle notification events.
  *
  * Created by alexsilva on 1/28/17.
  */
trait ContainerLifecycleListener {
  def onShutdown(container: ContainerService)

  def onStartup(container: ContainerService)
}
