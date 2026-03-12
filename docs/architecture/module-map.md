# IntentForge Module Map

## Core split

- `governance` 负责管
- `agent` 负责想
- `prompt/model/model-provider` 负责定义提示词、模型目录与模型提供方能力面
- `tool` 负责做
- `memory/config` 负责提供上下文
- `space` 负责公司、项目、产品、应用四级空间隔离、多资源绑定与继承解析
- `audit` 负责留痕
- `boot` 负责启动
- `desktop` 负责桌面端宿主与平台适配

## Root modules

| Module | Role |
| --- | --- |
| `intentforge-bom` | Unified dependency version alignment for the repository and external consumers |
| `intentforge-common` | Global shared enums, exceptions, constants, utils, and DTO bases |
| `intentforge-api` | External protocol contracts, REST DTOs, AG-UI events, request/response objects |
| `intentforge-governance` | Task orchestration, state machine, routing, strategy, coordination, scheduling |
| `intentforge-audit` | Run/step/tool-call records, event snapshots, replay, audit services |
| `intentforge-agent` | Agent abstraction family and runtime integrations |
| `intentforge-prompt` | Prompt definitions, registries, and pluggable prompt runtime |
| `intentforge-model` | Model catalogs, capability metadata, and pluggable model runtime |
| `intentforge-model-provider` | Model provider SPI, provider registries, and pluggable provider adapters |
| `intentforge-tool` | Tool SPI, connectors, execution, validation |
| `intentforge-memory` | Memory abstractions and implementations |
| `intentforge-config` | Config abstractions and layered providers |
| `intentforge-space` | Space hierarchy, inheritance resolution, and isolation bindings |
| `intentforge-boot` | Startup entrypoints for local and server modes |
| `intentforge-desktop` | Desktop host abstraction and platform adapters |

## Nested module families

### `intentforge-agent`

| Module | Role |
| --- | --- |
| `intentforge-agent-core` | `Task`, `Plan`, `ContextPack`, `Executor`, `Artifact`, `Decision` and coordination models |
| `intentforge-agent-native` | Native planner, coder, reviewer, judge implementations |
| `intentforge-agent-springai` | Spring AI based chat, advisor, and tool execution adapters |
| `intentforge-agent-external` | External runtime adapters such as Codex, Gemini, AgentScope |

### `intentforge-prompt`

| Module | Role |
| --- | --- |
| `intentforge-prompt-core` | Prompt definitions, variables, queries, registry SPI, manager-provider SPI |
| `intentforge-prompt-local` | In-memory prompt manager provider, classpath SPI loading, local plugin directory loading |

### `intentforge-model`

| Module | Role |
| --- | --- |
| `intentforge-model-core` | Model descriptors, capability catalog, registry SPI, manager-provider SPI |
| `intentforge-model-local` | In-memory model manager provider, classpath SPI loading, local plugin directory loading |

### `intentforge-model-provider`

| Module | Role |
| --- | --- |
| `intentforge-model-provider-core` | Provider descriptors, provider SPI, provider registry contracts, registry-provider SPI |
| `intentforge-model-provider-local` | In-memory provider registry provider, local plugin loading, metadata and version validation |

### `intentforge-tool`

| Module | Role |
| --- | --- |
| `intentforge-tool-core` | Tool SPI, gateway, registry, permission model |
| `intentforge-tool-mcp` | MCP client/server adapters and protocol wrappers |
| `intentforge-tool-connectors` | Git, Jira, repo, file, DB, and other connector entrypoints |
| `intentforge-tool-fs` | File read/write, patch, diff, snapshot, sandbox/worktree operations |
| `intentforge-tool-shell` | Process execution, stdout/stderr capture, timeout, interrupt, resource control |
| `intentforge-tool-validation` | Build, test, lint, rule checks, and acceptance validation |

### `intentforge-memory`

| Module | Role |
| --- | --- |
| `intentforge-memory-core` | `MemoryStore`, `MemoryStrategy`, `MemoryOrchestrator`, memory models |
| `intentforge-memory-file` | JSON, YAML, Markdown, NDJSON storage implementations |
| `intentforge-memory-sql` | SQLite and PostgreSQL working/episodic memory implementations |
| `intentforge-memory-graph` | Entity relations, dependency graph, graph retrieval |

### `intentforge-config`

| Module | Role |
| --- | --- |
| `intentforge-config-core` | `ConfigProvider`, `ConfigResolver`, overlay, config query models |
| `intentforge-config-file` | YAML/JSON local config loading and override |
| `intentforge-config-db` | Runtime and policy config persistence |
| `intentforge-config-graph` | Complex dependency and relation-driven config |

### `intentforge-space`

| Module | Role |
| --- | --- |
| `intentforge-space-core` | Company/project/product/application hierarchy definitions, resource binding profiles, SPI contracts, exceptions, and resolved profile models |
| `intentforge-space-local` | In-memory registry, multi-resource inheritance resolver, and local runtime assembly |

### `intentforge-boot`

| Module | Role |
| --- | --- |
| `intentforge-boot-local` | Local-first bootstrap, local storage initialization, and space-aware runtime wiring |
| `intentforge-boot-server` | Server deployment bootstrap and remote runtime mode |

### `intentforge-desktop`

| Module | Role |
| --- | --- |
| `intentforge-desktop-core` | Shared desktop host abstractions |
| `intentforge-desktop-windows` | Windows-specific host adapter and packaging entrypoint |
| `intentforge-desktop-macos` | macOS-specific host adapter and packaging entrypoint |
| `intentforge-desktop-linux` | Linux-specific host adapter and packaging entrypoint |

## Placeholder directories

| Path | Role |
| --- | --- |
| `frontend/desktop` | Electron + Vue rendering layer placeholder |
| `frontend/ui` | Shared frontend UI assets and component placeholder |
| `python/runtime` | Python runtime utilities and future bridge placeholder |
| `scripts` | Development and automation script placeholder |
