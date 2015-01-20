# Builtin Service Container Endpoints

## Overview

The endpoints below are built-in to the framework and provide some basic SaaS functionality. CIDR rules
are applied to some of the calls and are labeled in the table below.

## Available Endpoints

| Description           | Verb  | URL                           | Description   | Example   | CIDR Rules    |
| :-------------------- | :---- | :---------------------------- | :------------- | :--------- | :----------    |
| Ping                  | GET   | /ping                         | A simple ping that returns a timestamp              |           | No            |
| Health Check (Full)   | GET   | /health                  | This endpoint rolls up all registered health checkers and returns the service's status. The returns full documentation of the status.             |           | Yes           |
| Health Check (LB)     | GET   | /health/lb               | This endpoint performs the health check like the endpoint above, but returned a stripped down response. This is useful to use as a basis for Load Balancer health state calls.              |           | Yes           |
| Metrics               | GET   | /metrics                      | This endpoint returns the metrics data that has been collected y the service.              |           | Yes           |
| Shutdown              | POST  | /shutdown                     | This shuts down the service.              |           | Yes           |


## CIDR (Classless Inter-Domain Routing) Rules

These rules are used to allow/deny certain IPs from gaining access to HTTP endpoints. We default to using a range
of values to allow access:

* 127.0.0.1/30 -> 127.0.0.0 - 127.0.0.3
* 10.0.0.0/8 -> 10.0.0.0 - 10.255.255.255

The framework defines the default rules and placed in the reference.conf file.
The default settings are as follows and can be overridden by providing your own configuration settings.

```
# This section is used to support CIDR notation to allow or block calls to
# certain services
container {
  http {
    cidr {
      # This is a list of IP ranges to allow through. Can be empty.
      allow=["127.0.0.1/30", "10.0.0.0/8"]
      # This is a list of IP ranges to specifically deny access. Can be empty.
      deny=[]
    }
  }
}
```