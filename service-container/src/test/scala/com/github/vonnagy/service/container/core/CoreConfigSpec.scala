package com.github.vonnagy.service.container.core

import java.nio.file.Files

import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification

class CoreConfigSpec extends Specification {

  sequential

  "The config code" should {

    "Load the default configuration" in {
      val config = new CoreConfig {}.getConfig(None)
      config.getInt("container.http.port") must be equalTo 8080
    }

    "Use a passed config as the top level configuration" in {

      val config = new CoreConfig {}.getConfig(Some(ConfigFactory.parseString("container.http.port=9000")))
      config.getInt("container.http.port") must be equalTo 9000
    }

    "Parse an external file when set through a system property" in {

      val file = Files.write(Files.createTempFile("testconfig", ".conf").toAbsolutePath, "container.http.port=9000".getBytes)
      try {
         System.setProperty("config.file", file.toAbsolutePath.toString)

        val config = new CoreConfig {}.getConfig(None)
        config.getInt("container.http.port") must be equalTo 9000
      }
      finally {
        file.toFile.deleteOnExit()
        System.clearProperty("config.file")

      }

    }

  }
}
