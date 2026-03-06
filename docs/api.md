# API Reference

Base URL: `http://localhost:8080`

All endpoints except `/api/auth/**` require:
```
Authorization: Bearer <jwt-token>
```

All responses follow the unified envelope:
```json
{ "code": 200, "message": "success", "data": <payload> }
{ "code": 500, "message": "<error description>", "data": null }
```

---

## Authentication — `/api/auth`

Public endpoints; no token required.

### Register

```
POST /api/auth/register
Content-Type: application/json

{
  "username": "alice",
  "password": "secret123",
  "email": "alice@example.com"       // optional
}
```

**Response**
```json
{ "code": 200, "message": "success" }
```

**Errors**
| Code | Cause |
|---|---|
| 400 | Username already exists |

---

### Login

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "alice",
  "password": "secret123"
}
```

**Response**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzM4NCJ9...",
    "username": "alice"
  }
}
```

**Errors**
| Code | Cause |
|---|---|
| 401 | Bad credentials |

---

## Cluster Management — `/api/clusters`

### List Clusters

```
GET /api/clusters
```

**Response**
```json
{
  "code": 200,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "prod-cluster",
      "authType": "TOKEN",
      "apiServerUrl": "https://10.0.0.1:6443",
      "status": "CONNECTED",
      "statusMessage": "Connection successful (v1.29.0)",
      "defaultCluster": false,
      "createdAt": 1700000000
    }
  ]
}
```

---

### Add Cluster

```
POST /api/clusters
Content-Type: application/json
```

**Request body by auth type:**

**TOKEN**
```json
{
  "name": "my-cluster",
  "authType": "TOKEN",
  "apiServerUrl": "https://10.0.0.1:6443",
  "token": "eyJhbGci...",
  "skipTlsVerify": false,
  "caCertData": "LS0tLS1CRUdJTi..."    // optional
}
```

**CERT**
```json
{
  "name": "my-cluster",
  "authType": "CERT",
  "apiServerUrl": "https://10.0.0.1:6443",
  "clientCertData": "LS0tLS1CRUdJTi...",
  "clientKeyData": "LS0tLS1CRUdJTi...",
  "caCertData": "LS0tLS1CRUdJTi..."
}
```

**KUBECONFIG**
```json
{
  "name": "my-cluster",
  "authType": "KUBECONFIG",
  "kubeconfigContent": "apiVersion: v1\nclusters:\n..."
}
```

**IN_CLUSTER**
```json
{
  "name": "in-cluster",
  "authType": "IN_CLUSTER"
}
```

**Response**
```json
{
  "code": 200,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "my-cluster",
    "authType": "TOKEN",
    "status": "CONNECTED",
    "statusMessage": "Connection successful (v1.29.0)"
  }
}
```

---

### Test Connection (without saving)

```
POST /api/clusters/test
Content-Type: application/json

// Same body as Add Cluster
```

**Response** — returns `ClusterInfo` with connection result, no DB write.

---

### Delete Cluster

```
DELETE /api/clusters/{id}
```

**Response**
```json
{ "code": 200, "message": "success" }
```

---

## Kubernetes Browsing — `/api/k8s`

All endpoints accept an optional `?clusterId=<uuid>` query parameter.
If omitted, the user's default cluster is used.

### List Namespaces

```
GET /api/k8s/namespaces?clusterId=<uuid>
```

**Response**
```json
{
  "code": 200,
  "data": ["default", "kube-system", "production"]
}
```

---

### List Pods

```
GET /api/k8s/namespaces/{namespace}/pods?clusterId=<uuid>
```

**Response**
```json
{
  "code": 200,
  "data": [
    {
      "name": "my-app-5d7f9b-xk2p9",
      "namespace": "default",
      "status": "Running",
      "containers": ["app", "sidecar"],
      "nodeName": "node-1",
      "podIP": "10.244.1.5"
    }
  ]
}
```

---

### List Java Processes

```
GET /api/k8s/namespaces/{namespace}/pods/{pod}/containers/{container}/processes?clusterId=<uuid>
```

Executes `jps -l` inside the container.

**Response**
```json
{
  "code": 200,
  "data": [
    { "pid": 1, "mainClass": "com.example.Application" },
    { "pid": 234, "mainClass": "com.example.BatchJob" }
  ]
}
```

---

### Check Java Availability

```
GET /api/k8s/namespaces/{namespace}/pods/{pod}/containers/{container}/java?clusterId=<uuid>
```

**Response**
```json
{ "code": 200, "data": true }
```

---

## Arthas Diagnostics — `/api/arthas`

### List Commands

```
GET /api/arthas/commands
```

Returns the parameter schema for all 13 commands — used by the frontend to render forms dynamically.

**Response**
```json
{
  "code": 200,
  "data": [
    {
      "type": "jvm",
      "displayName": "JVM Info",
      "description": "Display JVM system properties...",
      "params": []
    },
    {
      "type": "watch",
      "displayName": "Watch",
      "description": "Observe method parameters...",
      "params": [
        {
          "name": "className",
          "label": "Class Name",
          "type": "STRING",
          "required": true,
          "defaultValue": null,
          "description": "Fully qualified class name"
        },
        ...
      ]
    }
  ]
}
```

---

### Version Matrix

```
GET /api/arthas/version-matrix
```

**Response**
```json
{
  "code": 200,
  "data": {
    "8":  { "recommended": "3.7.2", "supported": ["3.7.2", "3.6.9", "3.5.6", "3.4.8"] },
    "11": { "recommended": "3.7.2", "supported": ["3.7.2", "3.6.9", "3.5.6", "3.4.8"] },
    "17": { "recommended": "3.7.2", "supported": ["3.7.2", "3.6.9", "3.5.6"] },
    "21": { "recommended": "3.7.2", "supported": ["3.7.2"] }
  }
}
```

---

### Deploy Arthas

```
POST /api/arthas/deploy
Content-Type: application/json

{
  "clusterId": "550e8400-e29b-41d4-a716-446655440000",   // optional; default cluster if omitted
  "namespace": "default",
  "podName": "my-app-5d7f9b-xk2p9",
  "containerName": "app",
  "uploadJdk": true,        // upload a bundled JDK before deploying Arthas
  "jdkVersion": "17",       // required when uploadJdk=true
  "arthasVersion": "3.7.2"  // optional; uses server default when blank
}
```

**What happens:**
1. If `uploadJdk=true`: copies the JDK tar.gz from the server tool cache into the container and extracts it
2. Downloads the Arthas zip from the server tool cache into the container and extracts it

**Response**
```json
{ "code": 200, "message": "success" }
```

---

### Attach to JVM Process

```
POST /api/arthas/attach
Content-Type: application/json

{
  "clusterId": "550e8400-e29b-41d4-a716-446655440000",
  "namespace": "default",
  "podName": "my-app-5d7f9b-xk2p9",
  "containerName": "app",
  "pid": 1
}
```

**What happens:**
1. Executes `java -jar arthas-boot.jar <pid>` inside the container via `kubectl exec`
2. Sets up a `kubectl port-forward` to the Arthas HTTP API port
3. Calls Arthas `init_session` to obtain an internal session ID
4. Returns a `sessionId` for subsequent command calls

**Response**
```json
{
  "code": 200,
  "data": "a3f12bc4-90de-4f23-b8a1-1234567890ab"   // sessionId
}
```

---

### Execute Command

```
POST /api/arthas/execute
Content-Type: application/json

{
  "sessionId": "a3f12bc4-90de-4f23-b8a1-1234567890ab",
  "commandType": "watch",
  "params": {
    "className": "com.example.UserService",
    "methodName": "login",
    "express": "{params, returnObj}",
    "count": "3"
  }
}
```

**Response** — the raw JSON response from the Arthas HTTP API:
```json
{
  "code": 200,
  "data": {
    "state": "SUCCEEDED",
    "body": {
      "results": [
        {
          "type": "watch",
          "ts": 1700000000,
          "cost": 12.5,
          "value": "..."
        }
      ]
    }
  }
}
```

**Errors**
| Cause | Behaviour |
|---|---|
| Unknown `sessionId` | `400 IllegalArgumentException` |
| Unknown `commandType` | `400 IllegalArgumentException` |
| Arthas agent unreachable | `200` with `data.state = "FAILED"` |

---

### Close Session

```
DELETE /api/arthas/sessions/{sessionId}
```

Closes the Arthas internal session, terminates the port-forward process, and removes the session from memory.

**Response**
```json
{ "code": 200, "message": "success" }
```

---

## Tools — `/api/tools`

### List Cached JDK Packages

```
GET /api/tools/jdk
```

**Response**
```json
{
  "code": 200,
  "data": [
    { "version": "17", "fileName": "jdk-17-linux-x64.tar.gz", "size": 180000000 }
  ]
}
```

### List Cached Arthas Packages

```
GET /api/tools/arthas
```

**Response**
```json
{
  "code": 200,
  "data": [
    { "version": "3.7.2", "fileName": "arthas-packaging-3.7.2-bin.zip", "size": 15000000 }
  ]
}
```

### Download JDK

```
POST /api/tools/jdk/download
Content-Type: application/json

{ "version": "17" }
```

Downloads the JDK archive from the configured mirror into the local tool cache (`arthas.tools-dir`).

### Download Arthas

```
POST /api/tools/arthas/download
Content-Type: application/json

{ "version": "3.7.2" }
```

Downloads the Arthas distribution zip from Maven Central (or configured mirror) into the tool cache.

---

## Command Parameter Reference

### Parameter Types

| Type | Frontend Widget | Java Type |
|---|---|---|
| `STRING` | Text input | `String` |
| `INTEGER` | Number input | Parsed as `int` via `Integer.parseInt` |
| `BOOLEAN` | Switch / checkbox | Parsed as `Boolean.parseBoolean` |

> **Note:** `boolVal(params, key)` returns `false` when the key is absent from the request map. The `defaultValue` field on a `CommandParam` is metadata for the frontend pre-fill only — it is not used during command string construction. Integer params use explicit defaults in `intVal(params, key, defaultValue)`.

### All Commands with Parameters

#### `dashboard` — Dashboard
| Param | Type | Default | Description |
|---|---|---|---|
| `interval` | INTEGER | 5000 | Refresh interval in milliseconds |
| `count` | INTEGER | 1 | Number of refreshes before stopping |

Built command: `dashboard -i {interval} -n {count}`

---

#### `jvm` — JVM Info
No parameters. Always builds: `jvm`

---

#### `thread` — Thread
| Param | Type | Default | Description |
|---|---|---|---|
| `id` | INTEGER | — | Thread ID; if set, prints that thread's stack |
| `top` | INTEGER | — | Top N threads by CPU; used when `id` is blank |
| `deadlock` | BOOLEAN | false | Detect deadlocks (`thread -b`) |

Priority: `id` > `deadlock` > `top`

---

#### `watch` — Watch
| Param | Type | Default | Description |
|---|---|---|---|
| `className` | STRING | *(required)* | Fully qualified class name |
| `methodName` | STRING | *(required)* | Method to watch |
| `express` | STRING | `{params, returnObj, throwExp}` | OGNL expression |
| `condition` | STRING | — | OGNL filter condition |
| `count` | INTEGER | 5 | Max matches |
| `expand` | INTEGER | 1 | Object expand depth |
| `beforeInvoke` | BOOLEAN | false | Observe before invocation (`-b`) |
| `onException` | BOOLEAN | false | Observe on exception (`-e`) |

---

#### `trace` — Trace
| Param | Type | Default | Description |
|---|---|---|---|
| `className` | STRING | *(required)* | Class name |
| `methodName` | STRING | *(required)* | Method name |
| `condition` | STRING | — | OGNL filter condition |
| `count` | INTEGER | 5 | Max matches |
| `skipJdk` | BOOLEAN | false | Skip JDK internal calls (`--skipJDKMethod true`) |

---

#### `monitor` — Monitor
| Param | Type | Default | Description |
|---|---|---|---|
| `className` | STRING | *(required)* | Class name |
| `methodName` | STRING | *(required)* | Method name |
| `cycle` | INTEGER | 5 | Aggregation cycle in seconds (`-c`) |
| `count` | INTEGER | 10 | Max reporting cycles (`-n`) |

---

#### `stack` — Stack
| Param | Type | Default | Description |
|---|---|---|---|
| `className` | STRING | *(required)* | Class name |
| `methodName` | STRING | *(required)* | Method name |
| `condition` | STRING | — | OGNL filter condition |
| `count` | INTEGER | 5 | Max matches |

---

#### `heapdump` — Heap Dump
| Param | Type | Default | Description |
|---|---|---|---|
| `filePath` | STRING | `/tmp/arthas-heapdump.hprof` | Output path inside container |
| `liveOnly` | BOOLEAN | false | Dump only live objects (`--live`) |

---

#### `jad` — Decompile
| Param | Type | Default | Description |
|---|---|---|---|
| `className` | STRING | *(required)* | Class to decompile |
| `methodName` | STRING | — | Decompile specific method only |
| `source` | BOOLEAN | false | Show source only (`--source-only`) |

---

#### `sc` — Search Class
| Param | Type | Default | Description |
|---|---|---|---|
| `pattern` | STRING | *(required)* | Class name pattern (supports `*`) |
| `detail` | BOOLEAN | false | Show class details (`-d`) |
| `field` | BOOLEAN | false | Show field info (`-f`) |

---

#### `sm` — Search Method
| Param | Type | Default | Description |
|---|---|---|---|
| `className` | STRING | *(required)* | Class name pattern |
| `methodPattern` | STRING | — | Method name pattern (supports `*`) |
| `detail` | BOOLEAN | false | Show method details (`-d`) |

---

#### `ognl` — OGNL Expression
| Param | Type | Default | Description |
|---|---|---|---|
| `expression` | STRING | *(required)* | OGNL expression to evaluate |
| `expand` | INTEGER | 1 | Object expand depth (`-x`) |

---

#### `classloader` — Classloader
| Param | Type | Default | Description |
|---|---|---|---|
| `tree` | BOOLEAN | false | Show hierarchy tree (`-t`) |
| `stats` | BOOLEAN | false | Show statistics (`-l`) |
