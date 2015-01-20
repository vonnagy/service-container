package com.github.vonnagy.service.container.health

import org.joda.time.DateTime
import com.github.vonnagy.service.container.health.HealthState._


case class ContainerHealth(host: String,
                           applicationName: String,
                           applicationVersion: String,
                           containerVersion: String,
                           time: DateTime,
                           state: HealthState,
                           details: String,
                           checks: Seq[HealthInfo] = Seq.empty) {}
