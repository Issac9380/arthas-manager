# Arthas Manager

Kubernetes-based Java diagnostic platform. Deploys [Arthas](https://arthas.aliyun.com/) into containers and provides a visual GUI for JVM problem diagnosis — no command-line skills required.

## Features

- **Cluster Explorer** — Browse Kubernetes namespaces, pods, and containers
- **One-Click Deploy** — Upload JDK + Arthas boot jar to any container automatically
- **Process Attach** — List Java processes (`jps`) and attach Arthas with a single click
- **Visual Command Interface** — Form-based panels for every Arthas command; no raw commands needed
- **Supported Commands**: Dashboard, JVM Info, Thread, Watch, Trace, Monitor, Stack, Heap Dump, JAD, SC, SM, OGNL, Classloader
- **Real-Time Results** — Arthas HTTP API results streamed via WebSocket
- **Tool Cache** — Download/upload JDK archives and Arthas jars through the UI

## Architecture

```
arthas-manager/
├── backend/       Spring Boot 3.2 (Java 17)
└── frontend/      Vue 3 + Element Plus + Vite
```

### Backend Design Patterns

| Pattern | Where used |
|---|---|
| **Command** | `ArthasCommand` — each diagnostic command is a self-contained object |
| **Strategy** | Concrete command classes implement interchangeable `buildCommandString` logic |
| **Template Method** | `AbstractArthasCommand` provides shared param-extraction helpers |
| **Factory** | `ArthasCommandFactory` discovers all `@Component` commands via Spring injection |
| **Facade** | `ArthasServiceImpl` hides K8s exec, file upload, port-forward, and HTTP API complexity |
| **Singleton** | Session registry (`ArthasSessionManager`) maintains shared in-memory state |

### Key Components

```
backend/src/main/java/com/arthasmanager/
├── arthas/
│   ├── command/          ArthasCommand interface + AbstractArthasCommand
│   │   └── impl/         DashboardCommand, JvmCommand, WatchCommand, TraceCommand, ...
│   ├── factory/          ArthasCommandFactory (Factory pattern)
│   ├── executor/         ArthasCommandExecutor — Arthas HTTP API client
│   └── session/          ArthasSession + ArthasSessionManager
├── service/
│   ├── KubernetesService — namespace/pod/exec operations (Fabric8)
│   ├── FileTransferService — JDK/Arthas upload + port-forward setup
│   └── ArthasService — orchestration facade
├── controller/
│   ├── KubernetesController  GET /api/k8s/**
│   ├── ArthasController      POST /api/arthas/**
│   └── ToolsController       GET/POST /api/tools/**
└── websocket/
    └── ArthasWebSocketHandler  ws://host/ws/arthas
```

## Quick Start

### Prerequisites

- Java 17+, Maven 3.9+
- Node.js 18+
- Access to a Kubernetes cluster (`~/.kube/config`)

### Backend

```bash
cd backend
mvn spring-boot:run
# Starts on http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
npm run dev
# Opens on http://localhost:3000
```

### Usage Flow

1. **Tools page** — download JDK tar.gz and Arthas boot jar into the local cache
2. **Cluster page** — select a namespace → pod → container → click "查看 Java 进程"
3. Click **"诊断"** next to the target process — redirects to Diagnosis view
4. Click **"部署 Arthas"** (first time only) to upload the tools to the container
5. Click **"连接进程"** to attach Arthas and open a session
6. Use the left command menu to select a diagnostic command, fill in the form, and click **"执行命令"**

## Configuration

`backend/src/main/resources/application.yml`:

| Key | Default | Description |
|---|---|---|
| `kubernetes.kubeconfig` | `~/.kube/config` | Path to kubeconfig; blank = in-cluster |
| `arthas.tools-dir` | `~/.arthas-manager/tools` | Local tool cache directory |
| `arthas.boot-jar-url` | arthas.aliyun.com | Arthas boot jar download URL |
| `arthas.default-api-port` | `39394` | Arthas HTTP API port inside container |
| `arthas.session-timeout` | `30` | Session idle timeout (minutes) |
