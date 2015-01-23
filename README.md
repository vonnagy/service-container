Service Container
===========================

[![Build Status](https://travis-ci.org/vonnagy/service-container.png?branch=master)](https://travis-ci.org/vonnagy/service-container)
[![Coverage Status](https://coveralls.io/repos/vonnagy/service-container/badge.svg?branch=master)](https://coveralls.io/r/vonnagy/service-container?branch=master)

The service container is a lightweight framework that provides the ability to build micro-services. It contains a built-in Http server and utilizes both [Akka](http://akka.io/) and [Spray](http://spray.io/) at it's core.
The service container is fully SaaS ready as it provides functionality for recording metrics and for tracking the health of your service.

There are a few sub-projects in this repository and here is a list of those currently available.

* `service-container` : The core framework
* `service-container-metrics-reporting` : Support for external reporting of gathered metrics
* `service-container-examples` : A few examples that can be used for reference as well as run for testing

# How To

There are several aspects to utilizing the functionality contained within the Service Container and it's supporting libraries. This
section outlines the available functionality and how to best utilize the framework.

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