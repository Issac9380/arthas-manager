# Arthas Manager

Kubernetes-based Java diagnostic platform. Deploys [Arthas](https://arthas.aliyun.com/) into containers and provides a visual GUI for JVM diagnosis — no command-line skills required.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Supported Commands](#supported-commands)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Development Guide](#development-guide)

---

## Features

- **Multi-Cluster Management** — Add and manage multiple Kubernetes clusters; supports Token, Certificate, Kubeconfig, and in-cluster authentication
- **Cluster Explorer** — Browse namespaces, pods, and containers; detect Java processes and Java availability
- **One-Click Deploy** — Upload JDK + Arthas boot jar to any container automatically (no image rebuild needed)
- **Process Attach** — List Java processes (`jps`) and attach Arthas with a single click
- **Visual Command Interface** — Form-based panels for every Arthas command; no raw CLI needed
- **Real-Time Results** — Results from the Arthas HTTP API are displayed immediately in the UI
- **Tool Cache** — Download and manage JDK / Arthas package versions through the Tools page
- **User Authentication** — JWT-based login/register; all diagnostic sessions are user-scoped
- **Idle Session Eviction** — Sessions idle beyond the configured timeout are automatically cleaned up

---

## Architecture

```
arthas-manager/
├── backend/        Spring Boot 3.2 · Java 17 · Maven
└── frontend/       Vue 3 · Element Plus · Vite
```

### System Flow

```
Browser (Vue 3)
    │  REST /api/**  (JWT Bearer)
    ▼
Spring Boot Backend (8080)
    │  Fabric8 KubernetesClient
    ▼
Kubernetes API Server
    │  kubectl exec / port-forward
    ▼
Target Container
    └── Arthas HTTP API (port 39394, via local port-forward)
```

### Backend Design Patterns

| Pattern | Where used |
|---|---|
| **Command** | `ArthasCommand` — each diagnostic command is a self-contained object |
| **Strategy** | Concrete command classes implement interchangeable `buildCommandString` logic |
| **Template Method** | `AbstractArthasCommand` provides shared param-extraction helpers (`str`, `intVal`, `boolVal`, `appendFlag`) |
| **Factory** | `ArthasCommandFactory` auto-discovers all `@Component` commands via Spring injection |
| **Facade** | `ArthasServiceImpl` hides K8s exec, file upload, port-forward, and Arthas HTTP API complexity |
| **Registry** | `ArthasSessionManager` maintains in-memory session state with scheduled idle eviction |

---

## Quick Start

### Prerequisites

| Tool | Version |
|---|---|
| Java | 17+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| Kubernetes | Any cluster with API access |

### 1. Backend

```bash
cd backend
mvn spring-boot:run
# API available at http://localhost:8080
```

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
# UI available at http://localhost:3000  (proxies /api → :8080)
```

### 3. First Use

1. Open `http://localhost:3000` and register an account
2. **Tools page** — Download and cache the JDK tar.gz and Arthas boot jar
3. **Cluster page** — Add a Kubernetes cluster and browse namespaces → pods → containers
4. Click **"查看 Java 进程"** to list running Java processes in a container
5. Click **"诊断"** next to the target process to open the Diagnosis view
6. Click **"部署 Arthas"** (first time only) to upload tools into the container
7. Click **"连接进程"** to attach Arthas and establish a diagnostic session
8. Select a command from the left panel, fill in the form, and click **"执行命令"**

---

## Configuration

Edit `backend/src/main/resources/application.yml`:

| Key | Default | Description |
|---|---|---|
| `server.port` | `8080` | Backend HTTP port |
| `spring.datasource.url` | `~/.arthas-manager/arthas-manager.db` | SQLite database path |
| `jwt.secret` | *(change in production)* | HMAC-SHA384 signing key |
| `jwt.expiration` | `86400000` (24 h) | JWT validity in milliseconds |
| `arthas.tools-dir` | `~/.arthas-manager/tools` | Local cache for JDK/Arthas packages |
| `arthas.distribution-url` | Maven Central | Arthas zip download URL template (`{version}` placeholder) |
| `arthas.default-arthas-version` | `3.7.2` | Arthas version used when deploy request omits one |
| `arthas.default-api-port` | `39394` | Arthas HTTP API port inside the container |
| `arthas.session-timeout` | `30` | Session idle timeout in minutes |
| `kubernetes.kubeconfig` | `~/.kube/config` | Kubeconfig path; blank = in-cluster config |

> **Security note:** Change `jwt.secret` before deploying to production. Never commit real secrets.

---

## API Reference

All endpoints (except `/api/auth/**`) require `Authorization: Bearer <token>`.

### Authentication

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login; returns `{ token, username }` |

### Cluster Management

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/clusters` | List clusters for the current user |
| `POST` | `/api/clusters` | Add a new cluster |
| `POST` | `/api/clusters/test` | Test cluster connectivity without saving |
| `DELETE` | `/api/clusters/{id}` | Delete a cluster |

### Kubernetes Browsing

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/k8s/namespaces?clusterId=` | List namespaces |
| `GET` | `/api/k8s/namespaces/{ns}/pods?clusterId=` | List pods in a namespace |
| `GET` | `/api/k8s/namespaces/{ns}/pods/{pod}/containers/{container}/processes?clusterId=` | List Java processes (`jps`) |
| `GET` | `/api/k8s/namespaces/{ns}/pods/{pod}/containers/{container}/java?clusterId=` | Check if Java is available |

### Arthas Diagnostics

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/arthas/commands` | List all supported commands and their parameter schemas |
| `GET` | `/api/arthas/version-matrix` | JDK ↔ Arthas version compatibility matrix |
| `POST` | `/api/arthas/deploy` | Deploy Arthas (and optionally JDK) into a container |
| `POST` | `/api/arthas/attach` | Attach to a JVM process; returns `sessionId` |
| `POST` | `/api/arthas/execute` | Execute an Arthas command in an active session |
| `DELETE` | `/api/arthas/sessions/{sessionId}` | Close and clean up a session |

### Tools

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/tools/jdk` | List cached JDK packages |
| `GET` | `/api/tools/arthas` | List cached Arthas packages |
| `POST` | `/api/tools/jdk/download` | Download a JDK version into the local cache |
| `POST` | `/api/tools/arthas/download` | Download an Arthas version into the local cache |

### Request / Response Examples

**Login**
```json
POST /api/auth/login
{ "username": "admin", "password": "admin123" }

→ { "code": 200, "data": { "token": "eyJ...", "username": "admin" } }
```

**Deploy Arthas**
```json
POST /api/arthas/deploy
{
  "clusterId": "cluster-uuid",
  "namespace": "default",
  "podName": "my-app-pod",
  "containerName": "app",
  "uploadJdk": true,
  "jdkVersion": "17",
  "arthasVersion": "3.7.2"
}
```

**Execute Command**
```json
POST /api/arthas/execute
{
  "sessionId": "sess-uuid",
  "commandType": "jvm",
  "params": {}
}
```

---

## Supported Commands

| Command | Type Key | Description |
|---|---|---|
| Dashboard | `dashboard` | Real-time JVM metrics (threads, memory, GC) |
| JVM Info | `jvm` | JVM system properties, memory pools, class loading |
| Thread | `thread` | List threads or print stack trace; detect deadlocks |
| Watch | `watch` | Observe method parameters, return values, and exceptions |
| Trace | `trace` | Method call tree with per-node timing |
| Monitor | `monitor` | Method invocation statistics over a sampling period |
| Stack | `stack` | Print the call stack that triggers a method |
| Heap Dump | `heapdump` | Dump the JVM heap to an `.hprof` file |
| Decompile | `jad` | Decompile a loaded class back to Java source |
| Search Class | `sc` | Search loaded classes by pattern |
| Search Method | `sm` | Search methods of a loaded class |
| OGNL | `ognl` | Evaluate an OGNL expression in the JVM context |
| Classloader | `classloader` | Inspect classloader hierarchy and statistics |

---

## Project Structure

```
arthas-manager/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/arthasmanager/
│       │   │   ├── ArthasManagerApplication.java   # Entry point
│       │   │   ├── arthas/
│       │   │   │   ├── command/                    # ArthasCommand interface + AbstractArthasCommand
│       │   │   │   │   └── impl/                   # 13 concrete command implementations
│       │   │   │   ├── executor/                   # ArthasCommandExecutor (HTTP client)
│       │   │   │   ├── factory/                    # ArthasCommandFactory
│       │   │   │   ├── session/                    # ArthasSession + ArthasSessionManager
│       │   │   │   └── version/                    # ArthasVersionRegistry (JDK compatibility)
│       │   │   ├── config/                         # Security, DB, CORS, WebSocket config
│       │   │   ├── controller/                     # REST controllers
│       │   │   ├── entity/                         # JPA/MyBatis entities
│       │   │   ├── mapper/                         # MyBatis mappers
│       │   │   ├── model/                          # DTOs, VOs, enums
│       │   │   ├── security/                       # JWT filter, UserPrincipal, UserDetailsService
│       │   │   └── service/                        # Service interfaces + implementations
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── schema.sql                      # SQLite schema
│       │       └── mapper/                         # MyBatis XML mappers
│       └── test/
│           └── java/com/arthasmanager/             # Unit + integration tests (218 tests)
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── views/
│       │   ├── LoginView.vue                       # Authentication
│       │   ├── HomeView.vue                        # Landing / navigation
│       │   ├── ClusterView.vue                     # K8s namespace/pod/container browser
│       │   ├── DiagnosisView.vue                   # Main diagnostic UI
│       │   └── ToolsView.vue                       # JDK/Arthas package manager
│       ├── stores/
│       │   └── arthas.js                           # Pinia store (session + command state)
│       └── api/
│           └── index.js                            # Axios API client
├── CLAUDE.md                                       # AI assistant guide
└── README.md
```

---

## Testing

```bash
cd backend
mvn test
```

**Test coverage summary (218 tests, all passing):**

| Module | Test Class | Tests |
|---|---|---|
| Command helpers | `AbstractArthasCommandTest` | 19 |
| Dashboard command | `DashboardCommandTest` | 9 |
| Watch command | `WatchCommandTest` | 12 |
| JVM command | `JvmCommandTest` | 5 |
| Thread command | `ThreadCommandTest` | 9 |
| Trace command | `TraceCommandTest` | 7 |
| Monitor command | `MonitorCommandTest` | 6 |
| Stack command | `StackCommandTest` | 5 |
| Heap Dump command | `HeapDumpCommandTest` | 6 |
| JAD command | `JadCommandTest` | 6 |
| SC command | `ScCommandTest` | 6 |
| SM command | `SmCommandTest` | 6 |
| OGNL command | `OgnlCommandTest` | 5 |
| Classloader command | `ClassloaderCommandTest` | 6 |
| Command factory | `ArthasCommandFactoryTest` | 11 |
| Session model | `ArthasSessionTest` | 5 |
| Session manager | `ArthasSessionManagerTest` | 9 |
| HTTP executor | `ArthasCommandExecutorTest` | 6 |
| Result wrapper | `ResultTest` | 6 |
| Arthas service | `ArthasServiceImplTest` | 12 |
| Cluster service | `ClusterServiceImplTest` | 11 |
| User service | `UserServiceImplTest` | 6 |
| Arthas controller | `ArthasControllerTest` | 5 |
| Auth controller | `AuthControllerTest` | 3 |
| Cluster controller | `ClusterControllerTest` | 7 |
| K8s controller | `KubernetesControllerTest` | 7 |

---

## Development Guide

### Build

```bash
# Backend — compile and package
cd backend && mvn package -DskipTests

# Frontend — production build
cd frontend && npm run build
```

### Cluster Authentication Types

| Type | Required fields |
|---|---|
| `TOKEN` | `apiServerUrl`, `token` |
| `CERT` | `apiServerUrl`, `clientCertData`, `clientKeyData`, `caCertData` |
| `KUBECONFIG` | `kubeconfigContent` (raw YAML string) |
| `IN_CLUSTER` | *(no fields needed — uses pod service account)* |

### Adding a New Arthas Command

1. Create a class in `backend/.../arthas/command/impl/` extending `AbstractArthasCommand`
2. Annotate it with `@Component`
3. Implement `getType()`, `getDisplayName()`, `getDescription()`, `getParams()`, and `buildCommandString()`
4. The command is automatically registered in `ArthasCommandFactory` via Spring injection
5. Write a test class in the corresponding test package

### Key Dependencies

| Dependency | Purpose |
|---|---|
| Spring Boot 3.2 | Framework, security, web |
| Fabric8 Kubernetes Client | K8s API, exec, port-forward |
| Apache HttpClient 5 | Arthas HTTP API calls |
| MyBatis + SQLite | Persistence |
| Lombok | Boilerplate reduction |
| JJWT | JWT creation and validation |
| Vue 3 + Element Plus | Frontend UI |
| Pinia | Frontend state management |
| Vite | Frontend build tool |
