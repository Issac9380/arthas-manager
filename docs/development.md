# Development Guide

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| Java | 17+ | Backend runtime |
| Maven | 3.9+ | Backend build |
| Node.js | 18+ | Frontend build |
| npm | 9+ | Frontend package manager |
| kubectl | any | K8s operations (runtime) |

---

## Local Development Setup

### 1. Clone and build

```bash
git clone <repo-url>
cd arthas-manager
```

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
# API available at http://localhost:8080
# SQLite DB created at ~/.arthas-manager/arthas-manager.db
```

### 3. Start the frontend

```bash
cd frontend
npm install
npm run dev
# UI available at http://localhost:3000
# /api requests proxied to http://localhost:8080
```

---

## Build for Production

```bash
# Backend — produces backend/target/arthas-manager-backend-*.jar
cd backend && mvn package -DskipTests

# Frontend — outputs dist/ (serve with nginx or embed in Spring Boot static/)
cd frontend && npm run build
```

---

## Running Tests

```bash
cd backend
mvn test                          # run all 218 tests
mvn test -Dtest=WatchCommandTest  # run a single test class
mvn test -pl . -q                 # quiet output
```

Test results are written to `backend/target/surefire-reports/`.

---

## Project Conventions

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary>

[optional body]
```

| Type | When to use |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `test` | Adding or fixing tests |
| `refactor` | Code restructuring without behaviour change |
| `chore` | Build scripts, dependencies |
| `ci` | CI/CD config |

Example:
```
feat(arthas): add new RedefineCommand for hot-swapping class bytecode
```

### Branch Naming

```
feature/<short-description>
fix/<short-description>
chore/<short-description>
claude/<session-id>             ← AI-session branches
```

---

## Adding a New Arthas Command

**5 steps — no wiring code needed:**

### Step 1 — Create the command class

```java
// backend/src/main/java/com/arthasmanager/arthas/command/impl/RedefineCommand.java

@Component
public class RedefineCommand extends AbstractArthasCommand {

    @Override
    public String getType() { return "redefine"; }              // unique key

    @Override
    public String getDisplayName() { return "Redefine Class"; } // shown in UI

    @Override
    public String getDescription() {
        return "Hot-swap a class without restarting the JVM.";
    }

    @Override
    public List<CommandParam> getParams() {
        return List.of(
            CommandParam.builder()
                .name("classFilePath")
                .label("Class File Path")
                .type(CommandParam.ParamType.STRING)
                .required(true)
                .description("Absolute path of the .class file inside the container")
                .build()
        );
    }

    @Override
    public String buildCommandString(Map<String, Object> params) {
        return "redefine " + str(params, "classFilePath");
    }
}
```

### Step 2 — Write the test

```java
// backend/src/test/java/com/arthasmanager/arthas/command/impl/RedefineCommandTest.java

class RedefineCommandTest {

    private RedefineCommand command = new RedefineCommand();

    @Test void getType_returnsRedefine() {
        assertThat(command.getType()).isEqualTo("redefine");
    }

    @Test void buildCommandString_prependsRedefine() {
        Map<String, Object> params = Map.of("classFilePath", "/tmp/Foo.class");
        assertThat(command.buildCommandString(params))
            .isEqualTo("redefine /tmp/Foo.class");
    }
}
```

### Step 3 — Done

The `@Component` annotation causes Spring to auto-inject the new command into `ArthasCommandFactory`. It appears in `GET /api/arthas/commands` automatically and the frontend renders a dynamic form for it.

---

## `AbstractArthasCommand` Helper Reference

```java
// Extract a string value; returns "" if key absent or value null/blank
String str(Map<String, Object> params, String key)

// Extract a string value with a fallback default
String str(Map<String, Object> params, String key, String defaultValue)

// Parse an integer; returns defaultValue on parse failure or missing key
int intVal(Map<String, Object> params, String key, int defaultValue)

// Parse a boolean; returns false when key is absent
// Note: "true" → true, anything else → false
boolean boolVal(Map<String, Object> params, String key)

// Append " flag value" to sb; e.g. append(sb, "-n", "5") → " -n 5"
void append(StringBuilder sb, String flag, String value)

// Append " flag" to sb only when condition is true; e.g. appendFlag(sb, "-b", before)
void appendFlag(StringBuilder sb, String flag, boolean condition)
```

---

## Adding a New REST Endpoint

1. Add a method to the relevant `Service` interface in `backend/.../service/`
2. Implement it in the corresponding `*Impl` class in `service/impl/`
3. Add a controller method in the relevant `*Controller` class
4. Write a `@WebMvcTest`-based test for the controller method
5. Write a `@ExtendWith(MockitoExtension.class)` unit test for the service method

---

## Configuration for Different Environments

Override any `application.yml` key with:

**Environment variable** (replace `.` and `-` with `_`, uppercase):
```bash
export ARTHAS_DEFAULT_ARTHAS_VERSION=3.6.9
export JWT_SECRET=my-production-secret-key
```

**JVM system property:**
```bash
java -Darthas.default-arthas-version=3.6.9 -jar arthas-manager-backend.jar
```

**Spring profile** (`application-prod.yml`):
```yaml
jwt:
  secret: ${JWT_SECRET}
arthas:
  default-arthas-version: 3.7.2
```
```bash
java -Dspring.profiles.active=prod -jar arthas-manager-backend.jar
```

---

## Kubernetes Deployment Notes

### Required Permissions

The backend pod (or the kubeconfig/token used) needs these RBAC permissions:

```yaml
rules:
  - apiGroups: [""]
    resources: ["namespaces", "pods", "pods/exec", "pods/portforward"]
    verbs: ["get", "list", "create"]
```

### In-Cluster Auth

When running inside a Kubernetes pod, leave `kubernetes.kubeconfig` blank:

```yaml
kubernetes:
  kubeconfig: ""    # uses /var/run/secrets/kubernetes.io/serviceaccount
```

### Container Requirements

The target container must have:
- A running JVM process
- `java` on `$PATH` (or a bundled JDK uploaded via the Tools page)
- Writable `/tmp` (for Arthas working files)
- No `readOnlyRootFilesystem: true` security context (Arthas writes to disk)

---

## Troubleshooting

### Backend fails to start — `path ... does not exist`

The `~/.arthas-manager/` directory is created automatically in `main()` before Spring starts. If you see this error, it means an older build is being run. Rebuild with `mvn package`.

### `Connection refused` when executing commands

The port-forward is set up on attach. If the pod is restarted after attach, the port-forward dies. Close the session and attach again.

### JWT `SignatureException`

The `jwt.secret` in `application.yml` is a placeholder. In production, set a strong secret via environment variable and do not change it across restarts (existing tokens become invalid).

### Frontend proxy not working

Ensure the Vite dev server is running (`npm run dev`) and that `vite.config.js` has the proxy pointing to `http://localhost:8080`.

### `kubectl port-forward` hangs or fails

- Confirm `kubectl` is on `$PATH` of the user running the backend
- Confirm the target pod is in `Running` state
- Confirm Arthas has fully started before the port-forward is attempted (the backend waits briefly after exec)
