# Architecture & Design

## Overview

Arthas Manager is a three-tier web application that bridges a browser-based UI to the [Arthas](https://arthas.aliyun.com/) Java diagnostic agent running inside Kubernetes containers.

```
┌─────────────────────────────────────────────────────────────┐
│  Browser (Vue 3 + Element Plus)                             │
│  ┌──────────┐ ┌────────────┐ ┌──────────┐ ┌─────────────┐ │
│  │  Login   │ │  Cluster   │ │ Diagnosis│ │    Tools    │ │
│  │   View   │ │   View     │ │   View   │ │    View     │ │
│  └──────────┘ └────────────┘ └──────────┘ └─────────────┘ │
│         Pinia Store  ←→  Axios (JWT Bearer)                 │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP REST /api/**
┌───────────────────────────▼─────────────────────────────────┐
│  Spring Boot Backend (port 8080)                            │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ AuthController│  │ArthasControl.│  │ClusterController │  │
│  │ /api/auth/** │  │/api/arthas/**│  │ /api/clusters/** │  │
│  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
│         │                 │                    │            │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌────────▼─────────┐  │
│  │ UserService  │  │ArthasService │  │  ClusterService  │  │
│  │  (JWT+BCrypt)│  │   (Facade)   │  │ (K8s client mgmt)│  │
│  └──────────────┘  └──────┬───────┘  └────────┬─────────┘  │
│                            │                    │            │
│  ┌─────────────────────────▼────────────────────▼─────────┐  │
│  │              Fabric8 KubernetesClient                  │  │
│  └─────────────────────────┬───────────────────────────────┘  │
│                             │ kubectl exec / port-forward    │
└─────────────────────────────┼───────────────────────────────┘
                              │ Kubernetes API Server
┌─────────────────────────────▼───────────────────────────────┐
│  Target Pod / Container                                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Java Process (PID N)                                │  │
│  │    └── Arthas Agent (attached via VirtualMachine)    │  │
│  │          └── HTTP API  :39394/api                    │  │
│  └──────────────────────────────────────────────────────┘  │
│  (port-forward: localhost:random → container:39394)         │
└─────────────────────────────────────────────────────────────┘
```

---

## Backend Layer Design

### Design Patterns

#### 1. Command Pattern — `ArthasCommand`

Every Arthas diagnostic command is represented as a self-contained object implementing `ArthasCommand`:

```
ArthasCommand (interface)
└── AbstractArthasCommand (abstract base — param helpers)
    ├── DashboardCommand
    ├── JvmCommand
    ├── ThreadCommand
    ├── WatchCommand
    ├── TraceCommand
    ├── MonitorCommand
    ├── StackCommand
    ├── HeapDumpCommand
    ├── JadCommand
    ├── ScCommand
    ├── SmCommand
    ├── OgnlCommand
    └── ClassloaderCommand
```

Each command is responsible for:
- Declaring its parameter schema (`getParams()`) — used by the frontend to render forms dynamically
- Building the Arthas CLI command string (`buildCommandString(params)`) — sent to the HTTP API

#### 2. Factory Pattern — `ArthasCommandFactory`

All `@Component` command implementations are auto-injected by Spring into a `List<ArthasCommand>` and indexed by type at startup:

```java
// Auto-discovered — no registration code needed when adding a new command
@Component
public class ArthasCommandFactory {
    private final Map<String, ArthasCommand> commandMap;

    public ArthasCommandFactory(List<ArthasCommand> commands) {
        commandMap = commands.stream()
            .collect(Collectors.toMap(ArthasCommand::getType, c -> c));
    }
}
```

#### 3. Facade Pattern — `ArthasServiceImpl`

The service hides the multi-step complexity behind a clean interface:

```
deploy()  →  uploadJdk? → deployArthas (kubectl cp + exec)
attach()  →  startArthasAndPortForward → initSession → put(session)
execute() →  get(session) → buildCommandString → HTTP POST /api
close()   →  closeSession → session.close() → remove(session)
```

#### 4. Registry Pattern — `ArthasSessionManager`

In-memory session store backed by `ConcurrentHashMap<String, ArthasSession>`:
- `get(sessionId)` — retrieves and updates `lastUsedAt` timestamp
- `evictIdleSessions()` — `@Scheduled` task removes sessions idle beyond `session-timeout` minutes, closes port-forwards

---

## Session Lifecycle

```
POST /api/arthas/deploy
  └── FileTransferService.uploadJdk()          ← kubectl cp jdk.tar.gz
  └── FileTransferService.deployArthas()       ← kubectl cp arthas.zip + exec unzip

POST /api/arthas/attach  →  sessionId
  └── FileTransferService.startArthasAndPortForward()
        └── kubectl exec: java -jar arthas-boot.jar <pid>
        └── kubectl port-forward pod:39394 → localhost:<random>
  └── ArthasCommandExecutor.initSession()      ← POST /api {action:init_session}
  └── ArthasSessionManager.put(session)

POST /api/arthas/execute  (sessionId + commandType + params)
  └── ArthasCommandFactory.getCommand(type)
  └── command.buildCommandString(params)       ← "trace com.Foo bar -n 5"
  └── ArthasCommandExecutor.execute()          ← POST /api {action:exec, command:...}

DELETE /api/arthas/sessions/{sessionId}
  └── ArthasCommandExecutor.closeSession()     ← POST /api {action:close_session}
  └── ArthasSession.close()                    ← terminates port-forward process
  └── ArthasSessionManager.remove(sessionId)
```

---

## Security Architecture

- **Stateless** — no HTTP sessions; Spring Security configured with `STATELESS` session policy
- **JWT** — HMAC-SHA384 signed tokens, configurable expiration (default 24 h)
- **BCrypt** — passwords hashed with BCryptPasswordEncoder before storage
- **Filter chain** — `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`
- **Public endpoints** — only `/api/auth/register` and `/api/auth/login` are unauthenticated

```
Request
  └── JwtAuthenticationFilter
        ├── Extract Bearer token from Authorization header
        ├── Validate JWT signature and expiry
        ├── Load UserDetails from DB
        └── Set SecurityContextHolder authentication
  └── Controller (sees authenticated principal)
```

---

## Data Model

### SQLite Schema

```sql
CREATE TABLE users (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    username   TEXT    NOT NULL UNIQUE,
    password   TEXT    NOT NULL,          -- BCrypt hash
    email      TEXT,
    created_at INTEGER NOT NULL           -- Unix timestamp
);

CREATE TABLE clusters (
    id                 TEXT    PRIMARY KEY,   -- UUID
    user_id            INTEGER NOT NULL,
    name               TEXT    NOT NULL,
    auth_type          TEXT    NOT NULL,      -- TOKEN | CERT | KUBECONFIG | IN_CLUSTER
    api_server_url     TEXT,
    skip_tls_verify    INTEGER NOT NULL DEFAULT 0,
    token              TEXT,
    ca_cert_data       TEXT,
    client_cert_data   TEXT,
    client_key_data    TEXT,
    kubeconfig_content TEXT,
    default_cluster    INTEGER NOT NULL DEFAULT 0,
    status             TEXT,                  -- CONNECTED | DISCONNECTED | ERROR
    status_message     TEXT,
    created_at         INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id)
);
```

### In-Memory Session Model

```java
ArthasSession {
    String  sessionId;               // UUID, returned to caller
    String  clusterId;
    String  namespace;
    String  podName;
    String  containerName;
    int     pid;                     // target JVM process PID
    int     localPort;               // randomly assigned port-forward port
    String  arthasInternalSessionId; // assigned by Arthas HTTP API
    Instant createdAt;
    Instant lastUsedAt;              // updated on every get(); used for eviction
    Process portForwardProcess;      // kubectl port-forward handle
}
```

---

## Frontend Architecture

```
src/
├── views/
│   ├── LoginView.vue      — register / login forms; stores JWT in localStorage
│   ├── HomeView.vue       — navigation landing page
│   ├── ClusterView.vue    — cluster CRUD + K8s browser (ns → pod → container → process)
│   ├── DiagnosisView.vue  — main diagnostic UI: deploy / attach / command execution
│   └── ToolsView.vue      — download and manage JDK / Arthas packages
├── stores/
│   └── arthas.js          — Pinia store: cluster selection, session state, command history
└── api/
    └── index.js           — Axios instance; attaches Bearer token; handles 401 redirect
```

### State Flow in DiagnosisView

```
Select cluster → Select namespace → Select pod → Select container
  → listJavaProcesses()
    → Select PID → deploy() [first time]
      → attach() → sessionId stored in Pinia
        → Select command type → Fill form → execute()
          → Display JsonNode result in result panel
```

---

## JDK ↔ Arthas Version Compatibility

| JDK | Minimum Arthas | Reason |
|---|---|---|
| 8 | 3.4.x | All versions supported |
| 11 | 3.4.x | All versions supported |
| 17 | 3.5.4 | JDK strong encapsulation (JEP 396) |
| 21 | 3.7.0 | Virtual thread awareness |

The `ArthasVersionRegistry` encodes this matrix and exposes it via `GET /api/arthas/version-matrix` so the frontend can auto-select a compatible default.
