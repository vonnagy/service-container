Service Container
===========================

[![Build Status](https://travis-ci.org/vonnagy/service-container.png?branch=master)](https://travis-ci.org/vonnagy/service-container)
[![Coverage Status](https://coveralls.io/repos/vonnagy/service-container/badge.svg?branch=master)](https://coveralls.io/r/vonnagy/service-container?branch=master)
[<img src="https://img.shields.io/maven-central/v/com.github.vonnagy/service-container_2.10.svg?label=latest%20release%20for%202.10"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3Acom.github.vonnagy%20a%3Aservice-container_2.10)
[<img src="https://img.shields.io/maven-central/v/com.github.vonnagy/service-container_2.11.svg?label=latest%20release%20for%202.11"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3Acom.github.vonnagy%20a%3Aservice-container_2.11)

The service container is a lightweight framework that provides the ability to build microservices. It contains a built-in Http server and utilizes both [Akka](http://doc.akka.io/docs/akka/2.4/scala.html?_ga=1.42508261.1791590507.1484343870) and [Akka HTTP](http://doc.akka.io/docs/akka-http/current/scala.html) at it's core.
The service container is fully SaaS ready as it provides functionality for recording metrics and for tracking the health of your service.

There are a few sub-projects in this repository and here is a list of those currently available.

* `service-container` : The core framework
* `service-container-metrics-reporting` : Support for external reporting of gathered metrics
* `service-container-examples` : A few examples that can be used for reference as well as run for testing

# How To

There are several aspects to utilizing the functionality contained within the Service Container and it's supporting libraries. This
section outlines the available functionality and how to best utilize the framework.

### Configuration
As with any framework, it is important to allow custom configuration. The Service Container supports enhanced
configuration and follow the standard [Typesafe Config](https://github.com/typesafehub/config) rollup patterns.

[Instructions](docs/Configuration.md)

### Builtin Service Container endpoints

In order to allow our internal users and systems to self-discover the health of services, the Service Container will
expose canned endpoints. These endpoints can be used for such things as checking health, monitoring, and other various purposes.

[Instructions](docs/Endpoints.md)

### Metrics

The Service Container implements logic to track metrics within the system. It automatically tracks JVM and other internal metrics. The framework
also provides the ability to track any number of custom metrics. Once metrics are gathered, the can be accessed by the `/metrics` endpoint and
written out using any number of metrics reporters.

[Instructions](docs/Metrics.md)


### License

Copyright 2015 Ivan von Nagy

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
