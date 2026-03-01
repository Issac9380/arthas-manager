# CLAUDE.md — AI Assistant Guide for arthas-manager

## Project Overview

**arthas-manager** is a Kubernetes-based Java diagnostic platform. It deploys [Arthas](https://arthas.aliyun.com/) into containers, manages JDK uploads, and provides a visual GUI so operators can diagnose JVM issues without typing raw commands.

> This file is updated automatically as the project evolves. If you are an AI assistant, read this file carefully before making any changes.

---

## Repository State (as of 2026-03-01)

| Aspect | Status |
|---|---|
| Source code | ✅ Initial implementation added |
| Backend | Spring Boot 3.2 / Java 17 / Maven (`backend/`) |
| Frontend | Vue 3 + Element Plus + Vite (`frontend/`) |
| Package manager | Maven (backend), npm (frontend) |
| Build tooling | `mvn package` / `npm run build` |
| Tests | Not yet configured |
| CI/CD | Not configured |
| Deployment | Not configured |

## Build & Run Commands

```bash
# Backend
cd backend && mvn spring-boot:run        # dev server on :8080
cd backend && mvn package                # produces target/*.jar

# Frontend
cd frontend && npm install
cd frontend && npm run dev               # dev server on :3000 (proxies /api → :8080)
cd frontend && npm run build             # outputs dist/
```

---

## Git Workflow

### Branches

- `main` — stable, production-ready code (protected)
- `master` — initial commit branch (legacy; treat as equivalent to `main`)
- `claude/<session-id>` — AI-session feature branches (auto-created by Claude Code)

### Branch Naming Convention

```
<type>/<short-description>
```

Types: `feature/`, `fix/`, `chore/`, `docs/`, `refactor/`

### Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(optional-scope): <short summary>

[optional body]
```

Common types: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `ci`

Example:
```
feat(api): add Arthas agent connection endpoint
```

### Push Instructions for Claude Code Sessions

Claude Code AI sessions develop on a dedicated branch and push with:

```bash
git push -u origin claude/<session-id>
```

Never push directly to `main`/`master` without explicit user permission.

---

## Development Conventions (to be adopted as the project grows)

These conventions should be followed once code is added:

### Code Style

- Prefer explicit over implicit
- Write self-documenting code; add comments only where logic is non-obvious
- Keep functions small and single-purpose
- Avoid premature abstractions — create helpers only when used 3+ times

### Security

- Never hard-code credentials, tokens, or secrets
- Validate all external inputs at system boundaries
- Follow OWASP Top 10 guidelines
- Use environment variables for configuration; document required variables in README

### Dependencies

- Keep dependencies minimal and justified
- Pin dependency versions in lock files
- Review transitive dependencies for security issues before adding new packages

### Testing

- Write tests alongside new features (not after the fact)
- Aim for meaningful coverage of business logic, not 100% line coverage
- Unit tests for pure functions; integration tests for API/service boundaries

---

## About Arthas

[Arthas](https://arthas.aliyun.com/) is a Java application diagnostic tool from Alibaba that allows:

- Real-time monitoring of JVM state
- Method tracing and profiling without code modification
- Decompiling loaded classes
- Modifying class bytecode at runtime (hot-swap)
- Diagnosing production issues without restarting the JVM

**arthas-manager** likely provides a management UI or API layer to coordinate multiple Arthas agents running on different JVM instances.

---

## Key Files & Modules

| Path | Purpose |
|---|---|
| `README.md` | Project overview and quick-start guide |
| `CLAUDE.md` | This file — AI assistant guide |
| `backend/pom.xml` | Maven dependencies (Spring Boot, Fabric8, HttpClient5, Lombok) |
| `backend/src/main/resources/application.yml` | App configuration |
| `backend/.../arthas/command/ArthasCommand.java` | Strategy/Command interface |
| `backend/.../arthas/command/impl/` | Concrete commands: Dashboard, JVM, Thread, Watch, Trace, Monitor, Stack, HeapDump, JAD, SC, SM, OGNL, Classloader |
| `backend/.../arthas/factory/ArthasCommandFactory.java` | Factory — auto-discovers all command beans |
| `backend/.../arthas/session/ArthasSessionManager.java` | In-memory session registry with idle eviction |
| `backend/.../arthas/executor/ArthasCommandExecutor.java` | HTTP client for Arthas API |
| `backend/.../service/ArthasServiceImpl.java` | Facade: deploy → attach → execute → close |
| `backend/.../service/impl/KubernetesServiceImpl.java` | Fabric8 K8s operations |
| `backend/.../service/impl/FileTransferServiceImpl.java` | JDK/Arthas upload + port-forward |
| `frontend/src/views/DiagnosisView.vue` | Main diagnosis UI: command forms + result viewer |
| `frontend/src/views/ClusterView.vue` | K8s namespace/pod/container browser |
| `frontend/src/stores/arthas.js` | Pinia store for session + command state |

---

## Working with This Repository as an AI Assistant

### Before Making Changes

1. Read this file completely
2. Check the current branch (`git branch --show-current`)
3. Review any existing code relevant to the task
4. Understand the request scope — avoid over-engineering

### When Implementing Features

1. Work on the designated branch (never `main`/`master` directly)
2. Make small, focused commits with descriptive messages
3. Do not add unrequested features, refactors, or comments
4. Do not create unnecessary files
5. Prefer editing existing files over creating new ones

### When the Codebase Grows

Update this CLAUDE.md to reflect:
- New build commands (`npm run build`, `mvn package`, etc.)
- Test commands
- Environment variable requirements
- Architecture decisions
- Key module descriptions

---

## Updating This File

This CLAUDE.md should be updated whenever:
- New tooling or frameworks are added
- Build/test/run commands change
- Important architectural decisions are made
- New conventions are established

Keep this file concise and accurate. Remove stale information promptly.
