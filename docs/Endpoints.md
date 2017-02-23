# Builtin Service Container Endpoints

## Overview

The endpoints below are built-in to the framework and provide some basic SaaS functionality. CIDR rules
are applied to some of the calls and are labeled in the table below.

## Available Endpoints

The endpoints described below are built into the container and are always available. Example output is provided
after the table.

| Description           | Verb  | URL           | Description    | HTTP Status | CIDR Rules  |
| :-------------------- | :---- | :------------ | :------------- | :---------- | :---------- |
| Ping                  | GET   | /ping         | A simple ping that returns a timestamp  | 200 | No   |
| Health Check (Full)   | GET   | /health       | This endpoint rolls up all registered health checkers and returns the service's status. The returns full documentation of the status. | 200/503  | Yes  |
| Health Check (LB)     | GET   | /health/lb    | This endpoint performs the health check like the endpoint above, but returned a stripped down response. This is useful to use as a basis for Load Balancer health state calls. | 200/503 | Yes  |
| Metrics               | GET   | /metrics      | This endpoint returns the metrics data that has been collected y the service. | 200  | Yes  |
| Shutdown              | POST  | /shutdown     | This shuts down the service.       | 200       | Yes  |


### Ping
```
200 [plain/text] 

pong: 2015-01-23T15:11:21.805Z
```

### Health Check (full)
```
200 [application/json]

{
  "host": "my-host",
  "applicationName": "Container Service",
  "applicationVersion": "1.0.0.N/A",
  "containerVersion": "1.0.0.N/A",
  "time": "2015-01-23T15:11:50Z",
  "state": "OK",
  "details": "All sub-systems report perfect health",
  "checks": [
    {
      "name": "services",
      "state": "OK",
      "details": "Currently managing 2 services (including http)",
      "checks": [
        {
          "name": "http",
          "state": "OK",
          "details": "Currently connected on localhost:9092",
          "checks": []
        }
      ]
    },
    {
      "name": "metrics-reporting",
      "state": "OK",
      "details": "The system is currently managing 1 metrics reporters",
      "extra": [
        "com.github.vonnagy.service.container.metrics.reporting.Slf4jReporter"
      ]
      "checks": []
    }
  ]
}
```

### Health Check (load balancer)
```
200 [plain/text]

UP
```

### Metrics
```
200 [application/json]

{
  "system": {
    "jvm": {
      "buffers": {
        "jvm.buffer-pool.direct.capacity": 262144,
        "jvm.buffer-pool.direct.count": 2,
        "jvm.buffer-pool.direct.used": 262144,
        "jvm.buffer-pool.mapped.capacity": 0,
        "jvm.buffer-pool.mapped.count": 0,
        "jvm.buffer-pool.mapped.used": 0
      },
      "garbage-collection": {
        "jvm.gc.PS-MarkSweep.count": 0,
        "jvm.gc.PS-MarkSweep.time": 0,
        "jvm.gc.PS-Scavenge.count": 3,
        "jvm.gc.PS-Scavenge.time": 25
      },
      "memory": {
        "jvm.memory.heap.committed": 257425408,
        "jvm.memory.heap.init": 268435456,
        "jvm.memory.heap.max": 3817865216,
        "jvm.memory.heap.usage": 0.009573722992320533,
        "jvm.memory.heap.used": 36551184,
        "jvm.memory.non-heap.committed": 34013184,
        "jvm.memory.non-heap.init": 24576000,
        "jvm.memory.non-heap.max": 136314880,
        "jvm.memory.non-heap.usage": 0.237582514836238,
        "jvm.memory.non-heap.used": 32386032,
        "jvm.memory.pools.Code-Cache.usage": 0.020242055257161457,
        "jvm.memory.pools.PS-Eden-Space.usage": 0.017576433992509993,
        "jvm.memory.pools.PS-Old-Gen.usage": 2.7688792546008055E-4,
        "jvm.memory.pools.PS-Perm-Gen.usage": 0.3648061984922828,
        "jvm.memory.pools.PS-Survivor-Space.usage": 0.9971778506324405,
        "jvm.memory.total.committed": 291438592,
        "jvm.memory.total.init": 293011456,
        "jvm.memory.total.max": 3954180096,
        "jvm.memory.total.used": 69167216
      },
      "thread-states": {
        "jvm.thread.blocked.count": 0,
        "jvm.thread.count": 14,
        "jvm.thread.daemon.count": 3,
        "jvm.thread.deadlocks": null,
        "jvm.thread.new.count": 0,
        "jvm.thread.runnable.count": 4,
        "jvm.thread.terminated.count": 0,
        "jvm.thread.timed_waiting.count": 1,
        "jvm.thread.waiting.count": 9
      }
    },
    "metrics": {
      "container.http.connections.max-open": 2,
      "container.http.connections.open": 0,
      "container.http.connections.total": 4,
      "container.http.idle-timeouts": 0,
      "container.http.requests.max-open": 1,
      "container.http.requests.open": 0,
      "container.http.requests.total": 4,
      "container.http.uptime": 1310088
    }
  }
}
```

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
