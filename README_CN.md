# Arthas Manager

基于 Kubernetes 的 Java 在线诊断平台。将 [Arthas](https://arthas.aliyun.com/) 自动部署到容器中，并提供可视化 GUI，让运维人员无需输入任何命令即可完成 JVM 故障诊断。

---

## 目录

- [功能特性](#功能特性)
- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [API 接口](#api-接口)
- [支持的诊断命令](#支持的诊断命令)
- [项目结构](#项目结构)
- [测试说明](#测试说明)
- [开发指南](#开发指南)

---

## 功能特性

- **多集群管理** — 添加并管理多个 Kubernetes 集群，支持 Token、证书、Kubeconfig 及集群内认证四种方式
- **集群资源浏览** — 可视化浏览命名空间、Pod、容器，自动检测 Java 进程和 Java 运行环境
- **一键部署** — 自动将 JDK 和 Arthas 启动包上传到目标容器（无需重建镜像）
- **进程挂载** — 列出容器内 Java 进程（`jps`），一键附加 Arthas
- **可视化命令界面** — 所有 Arthas 命令均以表单形式呈现，无需手写命令
- **实时结果展示** — Arthas HTTP API 的返回结果直接呈现在界面中
- **工具包缓存** — 通过工具页面下载并管理 JDK / Arthas 版本包
- **用户认证** — 基于 JWT 的登录/注册，所有会话按用户隔离
- **空闲会话自动清理** — 超过配置时长未使用的会话自动回收并关闭端口转发

---

## 系统架构

```
arthas-manager/
├── backend/        Spring Boot 3.2 · Java 17 · Maven
└── frontend/       Vue 3 · Element Plus · Vite
```

### 系统调用链路

```
浏览器 (Vue 3)
    │  REST /api/**  (JWT Bearer Token)
    ▼
Spring Boot 后端 (:8080)
    │  Fabric8 KubernetesClient
    ▼
Kubernetes API Server
    │  kubectl exec / port-forward
    ▼
目标容器
    └── Arthas HTTP API (:39394，经本地端口转发访问)
```

### 后端设计模式

| 模式 | 应用位置 |
|---|---|
| **命令模式** | `ArthasCommand` — 每个诊断命令封装为独立对象 |
| **策略模式** | 各命令实现类各自负责 `buildCommandString` 逻辑 |
| **模板方法** | `AbstractArthasCommand` 提供公共参数提取工具方法 |
| **工厂模式** | `ArthasCommandFactory` 通过 Spring 注入自动发现所有命令 |
| **门面模式** | `ArthasServiceImpl` 屏蔽 K8s exec、文件上传、端口转发、HTTP API 的复杂性 |
| **注册表模式** | `ArthasSessionManager` 维护内存中的会话状态，定时清理空闲会话 |

---

## 快速开始

### 环境要求

| 工具 | 版本要求 |
|---|---|
| Java | 17+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| Kubernetes | 有 API 访问权限的任意集群 |

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
# API 地址: http://localhost:8080
```

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
# 页面地址: http://localhost:3000（/api 请求自动代理到 :8080）
```

### 3. 首次使用流程

1. 打开 `http://localhost:3000`，注册账号并登录
2. **工具页面** — 下载所需的 JDK 压缩包和 Arthas 启动包到本地缓存
3. **集群页面** — 添加 Kubernetes 集群，浏览命名空间 → Pod → 容器
4. 点击 **"查看 Java 进程"** 列出容器内正在运行的 Java 进程
5. 点击目标进程旁的 **"诊断"** 按钮，进入诊断视图
6. 首次使用时点击 **"部署 Arthas"**，将工具包上传到容器
7. 点击 **"连接进程"**，附加 Arthas 并建立诊断会话
8. 从左侧命令面板选择诊断命令，填写参数，点击 **"执行命令"**

---

## 配置说明

修改 `backend/src/main/resources/application.yml`：

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `server.port` | `8080` | 后端 HTTP 端口 |
| `spring.datasource.url` | `~/.arthas-manager/arthas-manager.db` | SQLite 数据库路径 |
| `jwt.secret` | *(生产环境请修改)* | JWT 签名密钥（HMAC-SHA384） |
| `jwt.expiration` | `86400000`（24小时） | JWT 有效期，单位毫秒 |
| `arthas.tools-dir` | `~/.arthas-manager/tools` | JDK / Arthas 包本地缓存目录 |
| `arthas.distribution-url` | Maven Central | Arthas 压缩包下载地址模板（含 `{version}` 占位符） |
| `arthas.default-arthas-version` | `3.7.2` | 部署请求未指定版本时使用的默认 Arthas 版本 |
| `arthas.default-api-port` | `39394` | 容器内 Arthas HTTP API 端口 |
| `arthas.session-timeout` | `30` | 会话空闲超时时间（分钟） |
| `kubernetes.kubeconfig` | `~/.kube/config` | kubeconfig 路径；留空则使用集群内配置 |

> **安全提示：** 生产环境部署前请务必修改 `jwt.secret`，切勿将密钥提交到代码仓库。

---

## API 接口

除 `/api/auth/**` 外，所有接口均需携带 JWT 认证头：
```
Authorization: Bearer <token>
```

所有响应统一格式：
```json
{ "code": 200, "message": "success", "data": <数据> }
{ "code": 500, "message": "<错误描述>", "data": null }
```

### 认证接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/auth/register` | 注册新用户 |
| `POST` | `/api/auth/login` | 登录，返回 JWT Token |

**登录示例：**
```json
// 请求
POST /api/auth/login
{ "username": "admin", "password": "admin123" }

// 响应
{ "code": 200, "data": { "token": "eyJ...", "username": "admin" } }
```

### 集群管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/clusters` | 获取当前用户的集群列表 |
| `POST` | `/api/clusters` | 添加集群 |
| `POST` | `/api/clusters/test` | 测试集群连通性（不保存） |
| `DELETE` | `/api/clusters/{id}` | 删除集群 |

**集群认证方式：**

| 认证类型 | 必填字段 |
|---|---|
| `TOKEN` | `apiServerUrl`、`token` |
| `CERT` | `apiServerUrl`、`clientCertData`、`clientKeyData`、`caCertData` |
| `KUBECONFIG` | `kubeconfigContent`（YAML 字符串） |
| `IN_CLUSTER` | 无需额外字段（使用 Pod ServiceAccount） |

### Kubernetes 资源浏览

所有接口支持可选参数 `?clusterId=<uuid>`，不传则使用默认集群。

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/k8s/namespaces` | 列出命名空间 |
| `GET` | `/api/k8s/namespaces/{ns}/pods` | 列出 Pod |
| `GET` | `/api/k8s/namespaces/{ns}/pods/{pod}/containers/{container}/processes` | 列出 Java 进程（`jps -l`） |
| `GET` | `/api/k8s/namespaces/{ns}/pods/{pod}/containers/{container}/java` | 检查是否存在 Java 环境 |

### Arthas 诊断

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/arthas/commands` | 获取所有命令的参数 Schema（前端动态渲染表单） |
| `GET` | `/api/arthas/version-matrix` | 获取 JDK ↔ Arthas 版本兼容矩阵 |
| `POST` | `/api/arthas/deploy` | 部署 Arthas（可选上传 JDK）到容器 |
| `POST` | `/api/arthas/attach` | 附加到 JVM 进程，返回 `sessionId` |
| `POST` | `/api/arthas/execute` | 在会话中执行诊断命令 |
| `DELETE` | `/api/arthas/sessions/{sessionId}` | 关闭并清理会话 |

**执行命令示例：**
```json
POST /api/arthas/execute
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

### 工具管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/tools/jdk` | 列出已缓存的 JDK 包 |
| `GET` | `/api/tools/arthas` | 列出已缓存的 Arthas 包 |
| `POST` | `/api/tools/jdk/download` | 下载指定版本 JDK 到本地缓存 |
| `POST` | `/api/tools/arthas/download` | 下载指定版本 Arthas 到本地缓存 |

---

## 支持的诊断命令

| 命令 | 类型键 | 功能说明 |
|---|---|---|
| 仪表盘 | `dashboard` | 实时展示线程、内存、GC、CPU 等 JVM 核心指标 |
| JVM 信息 | `jvm` | 查看 JVM 系统属性、内存池、类加载、GC 详情 |
| 线程 | `thread` | 列出所有线程 / 打印指定线程栈 / 检测死锁 |
| 方法观测 | `watch` | 实时观测方法的入参、返回值、异常信息 |
| 调用链路追踪 | `trace` | 输出方法调用树及各节点耗时，快速定位慢调用 |
| 方法监控 | `monitor` | 周期性统计方法调用次数、平均响应时间、失败率 |
| 调用堆栈 | `stack` | 打印触发指定方法的完整调用堆栈 |
| 堆内存转储 | `heapdump` | 将 JVM 堆转储为 `.hprof` 文件供离线分析 |
| 反编译 | `jad` | 将已加载的类反编译为 Java 源代码 |
| 查找类 | `sc` | 按名称模式查找已加载的类（支持通配符） |
| 查找方法 | `sm` | 查找已加载类的方法列表 |
| OGNL 表达式 | `ognl` | 在目标 JVM 中执行任意 OGNL 表达式（读取字段、调用方法等） |
| 类加载器 | `classloader` | 查看类加载器层次结构和统计信息 |

### JDK ↔ Arthas 版本兼容性

| JDK 版本 | 最低 Arthas 版本 | 原因 |
|---|---|---|
| 8 | 3.4.x | 全版本支持 |
| 11 | 3.4.x | 全版本支持 |
| 17 | 3.5.4 | JDK 强封装（JEP 396）需要适配 |
| 21 | 3.7.0 | 虚拟线程感知支持 |

---

## 项目结构

```
arthas-manager/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/arthasmanager/
│       │   │   ├── ArthasManagerApplication.java   # 启动入口（含数据库目录预创建）
│       │   │   ├── arthas/
│       │   │   │   ├── command/                    # ArthasCommand 接口 + AbstractArthasCommand
│       │   │   │   │   └── impl/                   # 13 个具体命令实现
│       │   │   │   ├── executor/                   # ArthasCommandExecutor（HTTP 客户端）
│       │   │   │   ├── factory/                    # ArthasCommandFactory（命令工厂）
│       │   │   │   ├── session/                    # ArthasSession + ArthasSessionManager
│       │   │   │   └── version/                    # ArthasVersionRegistry（版本兼容矩阵）
│       │   │   ├── config/                         # 安全、数据库、CORS 配置
│       │   │   ├── controller/                     # REST 控制器
│       │   │   ├── entity/                         # MyBatis 实体类
│       │   │   ├── mapper/                         # MyBatis Mapper 接口
│       │   │   ├── model/                          # DTO、VO、枚举
│       │   │   ├── security/                       # JWT 过滤器、UserPrincipal、UserDetailsService
│       │   │   └── service/                        # 服务接口及实现
│       │   └── resources/
│       │       ├── application.yml                 # 应用配置
│       │       ├── schema.sql                      # SQLite 建表语句
│       │       └── mapper/                         # MyBatis XML 映射文件
│       └── test/
│           └── java/com/arthasmanager/             # 单元测试 + 集成测试（218 个测试用例）
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── views/
│       │   ├── LoginView.vue                       # 登录/注册页
│       │   ├── HomeView.vue                        # 首页导航
│       │   ├── ClusterView.vue                     # 集群/资源浏览页
│       │   ├── DiagnosisView.vue                   # 诊断主页面
│       │   └── ToolsView.vue                       # 工具包管理页
│       ├── stores/
│       │   └── arthas.js                           # Pinia 状态管理（会话、命令状态）
│       └── api/
│           └── index.js                            # Axios 封装（自动附加 JWT、401 跳转）
├── docs/
│   ├── architecture.md                             # 架构设计文档（英文）
│   ├── api.md                                      # API 接口详细文档（英文）
│   └── development.md                              # 开发指南（英文）
├── CLAUDE.md                                       # AI 助手指引
├── README.md                                       # 项目说明（英文）
└── README_CN.md                                    # 项目说明（中文，本文件）
```

---

## 测试说明

```bash
cd backend
mvn test
```

**测试覆盖情况（218 个测试用例，全部通过）：**

| 模块 | 测试类 | 用例数 |
|---|---|---|
| 命令参数工具方法 | `AbstractArthasCommandTest` | 19 |
| Dashboard 命令 | `DashboardCommandTest` | 9 |
| Watch 命令 | `WatchCommandTest` | 12 |
| JVM 命令 | `JvmCommandTest` | 5 |
| Thread 命令 | `ThreadCommandTest` | 9 |
| Trace 命令 | `TraceCommandTest` | 7 |
| Monitor 命令 | `MonitorCommandTest` | 6 |
| Stack 命令 | `StackCommandTest` | 5 |
| HeapDump 命令 | `HeapDumpCommandTest` | 6 |
| JAD 命令 | `JadCommandTest` | 6 |
| SC 命令 | `ScCommandTest` | 6 |
| SM 命令 | `SmCommandTest` | 6 |
| OGNL 命令 | `OgnlCommandTest` | 5 |
| Classloader 命令 | `ClassloaderCommandTest` | 6 |
| 命令工厂 | `ArthasCommandFactoryTest` | 11 |
| 会话模型 | `ArthasSessionTest` | 5 |
| 会话管理器 | `ArthasSessionManagerTest` | 9 |
| HTTP 执行器 | `ArthasCommandExecutorTest` | 6 |
| 统一响应体 | `ResultTest` | 6 |
| Arthas 服务 | `ArthasServiceImplTest` | 12 |
| 集群服务 | `ClusterServiceImplTest` | 11 |
| 用户服务 | `UserServiceImplTest` | 6 |
| Arthas 控制器 | `ArthasControllerTest` | 5 |
| 认证控制器 | `AuthControllerTest` | 3 |
| 集群控制器 | `ClusterControllerTest` | 7 |
| K8s 控制器 | `KubernetesControllerTest` | 7 |

---

## 开发指南

### 编译打包

```bash
# 后端 — 生成 backend/target/arthas-manager-backend-*.jar
cd backend && mvn package -DskipTests

# 前端 — 生成 frontend/dist/（可用 nginx 托管或嵌入 Spring Boot static/）
cd frontend && npm run build
```

### 新增 Arthas 命令（5 步，无需注册代码）

1. 在 `backend/.../arthas/command/impl/` 下新建类，继承 `AbstractArthasCommand`
2. 添加 `@Component` 注解
3. 实现 `getType()`、`getDisplayName()`、`getDescription()`、`getParams()`、`buildCommandString()`
4. Spring 启动时自动将其注入 `ArthasCommandFactory`，`GET /api/arthas/commands` 自动返回新命令
5. 在对应测试包下编写测试类

### 常用工具方法（`AbstractArthasCommand`）

```java
str(params, "key")                    // 提取字符串，不存在时返回 ""
str(params, "key", "默认值")           // 提取字符串，不存在时返回默认值
intVal(params, "key", 5)              // 提取整数，解析失败或不存在时返回默认值
boolVal(params, "key")                // 提取布尔值，不存在时返回 false
appendFlag(sb, "--live", liveOnly)    // 条件为 true 时追加标志
```

### 环境变量覆盖配置

```bash
export JWT_SECRET=your-production-secret
export ARTHAS_DEFAULT_ARTHAS_VERSION=3.7.2
export ARTHAS_SESSION_TIMEOUT=60
```

### 常见问题

| 问题 | 原因 | 解决方法 |
|---|---|---|
| 启动报 `path ... does not exist` | 旧版本构建，目录预创建逻辑未包含 | 重新执行 `mvn package` |
| 执行命令时报 `Connection refused` | Pod 重启后端口转发断开 | 关闭会话后重新 Attach |
| JWT `SignatureException` | 服务重启后 `jwt.secret` 变更 | 统一通过环境变量设置密钥，保持稳定 |
| 前端代理不生效 | Vite 未启动 | 确认 `npm run dev` 正在运行 |

---

## 更多文档

| 文档 | 说明 |
|---|---|
| [docs/architecture.md](docs/architecture.md) | 系统架构与设计模式详解（英文） |
| [docs/api.md](docs/api.md) | 完整 API 接口参考（英文） |
| [docs/development.md](docs/development.md) | 开发环境搭建与贡献指南（英文） |
| [CLAUDE.md](CLAUDE.md) | AI 助手操作指引 |
