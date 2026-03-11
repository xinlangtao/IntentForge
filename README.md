# IntentForge

[English](README.md) | [Simplified Chinese](README.zh-CN.md)

> **Think it. Forge it.**  
> **Where intent arrives, systems can be forged.**

**IntentForge** is an **intent-driven intelligent development platform** built for the AI era.  
It is not another chatbot shell, nor a wrapper around a single Agent framework.  
It is an engineering platform core built around **Context Engineering, Spec-Driven architecture, cloud-native design, Agent governance, tool execution, contextual memory, observability, and auditability**.

You describe the intent.  
IntentForge forges that intent into **tasks, plans, context, execution, decisions, and artifacts**, ultimately delivering a real system directly.

---

## What is IntentForge

Most AI development tools focus on **“how to make a single agent better at doing things.”**  
IntentForge focuses on **“how multiple agents can reliably accomplish things under unified governance.”**

It is responsible for:

- Transforming **intent** into tasks, plans, and artifacts
- Deciding **who executes** and **how execution happens**
- Providing **multi-perspective visibility** into the execution process
- Performing **verification, auditing, replay, and knowledge accumulation**
- Enabling **unified governance and pluggability** for configuration, memory, rules, tools, and executors
- Managing both **native agents** and **external coding agents** under a single control plane

IntentForge is not a chat wrapper.  
It is an **AI-Native Development Platform**.

---

## Modules

- governance: responsible for governance
- agent: responsible for reasoning
- tool: responsible for execution
- memory/config: responsible for contextual data
- audit: responsible for traceability
- boot: responsible for startup
- desktop: responsible for desktop integration

```text
intentforge/
├─ intentforge-bom                         # Unified dependency versions and build baseline
├─ intentforge-common                      # Common foundational utilities: shared models, enums, constants, exceptions, utility classes
├─ intentforge-api                         # External protocol layer: DTOs, event models, AG-UI protocol objects
├─ intentforge-governance                  # Governance core: orchestration, state machines, coordination, scheduling, policy control
├─ intentforge-audit                       # Audit and replay: full trace of run/step/tool-call/decision
├─ intentforge-agent                       # Agent aggregation module
│  ├─ intentforge-agent-core               # Agent core abstractions: Task / Plan / ContextPack / Executor / Artifact / Decision
│  ├─ intentforge-agent-native             # Native in-house coding agent implementation
│  ├─ intentforge-agent-springai           # Executor implementation based on Spring AI
│  └─ intentforge-agent-external           # External agent adapters: opencode / codex / gemini / agentscope, etc.
├─ intentforge-tool                        # Tool aggregation module
│  ├─ intentforge-tool-core                # Tool SPI, ToolGateway, permission model, tool registry
│  ├─ intentforge-tool-mcp                 # MCP protocol integration and MCP tool bridging
│  ├─ intentforge-tool-connectors          # External connectors: Git / Jira / Repo / File / DB, etc.
│  ├─ intentforge-tool-fs                  # File system tools: read/write, diff, patch, snapshots, worktree/sandbox
│  ├─ intentforge-tool-shell               # Command execution tools: process management, stdout/stderr, timeout, interruption
│  └─ intentforge-tool-validation          # Validation tools: build, test, lint, rule validation, result verification
├─ intentforge-memory                      # Memory aggregation module
│  ├─ intentforge-memory-core              # Memory abstractions: MemoryStore, MemoryStrategy, MemoryOrchestrator
│  ├─ intentforge-memory-file              # File-based memory implementation: JSON / YAML / Markdown / NDJSON
│  ├─ intentforge-memory-sql               # SQL-based memory implementation: SQLite / PostgreSQL
│  └─ intentforge-memory-graph             # Graph-based memory implementation: entity relationships, knowledge dependencies, graph retrieval
├─ intentforge-config                      # Configuration aggregation module
│  ├─ intentforge-config-core              # Configuration abstractions: ConfigProvider, ConfigResolver, Overlay
│  ├─ intentforge-config-file              # File-based configuration: local YAML / JSON configuration
│  ├─ intentforge-config-db                # Database configuration: runtime configuration, policy configuration, layered configuration
│  └─ intentforge-config-graph             # Graph configuration: complex dependencies and relational rule configuration
├─ intentforge-boot                        # Boot aggregation module
│  ├─ intentforge-boot-local               # Local mode entry: desktop local core
│  └─ intentforge-boot-server              # Server mode entry: remote services and platform deployment
├─ intentforge-desktop                     # Desktop integration module
│  ├─ intentforge-desktop-core             # Desktop abstraction layer: IDE / terminal / directory / file integration
│  ├─ intentforge-desktop-windows          # Windows desktop integration
│  ├─ intentforge-desktop-macos            # macOS desktop integration
│  └─ intentforge-desktop-linux            # Linux desktop integration
├─ frontend                                # Frontend projects
│  ├─ desktop                              # Desktop shell frontend: Electron / Tauri container layer
│  └─ ui                                   # Common UI frontend: conversation, panels, artifacts, state visualization
├─ python                                  # Python runtime extensions
│  └─ runtime                              # Python runtime: supports specific AI/data/execution scenarios
└─ scripts                                 # Build, startup, release, and development helper scripts
```
