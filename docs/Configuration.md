# Configuration

## Overview
Configuration of the **Service Container** uses the standard [Typesafe Config](https://github.com/typesafehub/config)
rollup patterns. This template contains an `application.conf` file that overrides the default settings which are
contained in the dependent libraries.

This file can be edited or configuration can also be set by the following:

 - A JVM system property called `config.file` can be used (e.g. `-Dconfig.file=/myconfig.conf`)
 - Place an `application.conf` file in a **conf** sub-directory off the process root
 - Configure the service with a config during the build process
 (e.g. `val service = new ContainerBuilder().withConfig(ConfigFactory.parseFile("/somefile.conf"))`)

 The configuration for **Service Container** specific logic is contained within the configuration section `container`.

