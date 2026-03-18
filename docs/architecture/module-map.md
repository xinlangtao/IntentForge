# IntentForge Module Map

## Core split

- `governance` 负责管
- `agent` 负责想
- `channel` 负责连接外部消息渠道、账号实例、会话目标与路由入口
- `hook` 负责对外回调入口、协议适配与 hook 账号解析
- `prompt/model/model-provider` 负责定义提示词、模型目录与模型提供方能力面
- `tool` 负责做
- `memory/config` 负责提供上下文
- `session` 负责会话生命周期、消息历史与检索归档
- `space` 负责公司、项目、产品、应用四级空间隔离、多资源绑定与继承解析
- `audit` 负责留痕
- `boot` 负责启动
- `desktop` 负责桌面端宿主与平台适配

## Root modules

| Module | Role |
| --- | --- |
| `intentforge-bom` | Unified dependency version alignment for the repository and external consumers |
| `intentforge-common` | Global shared enums, exceptions, constants, shared validation/normalization utils, and DTO bases |
| `intentforge-api` | External protocol contracts, REST DTOs, transport-neutral API controllers and application services, checkpoint-transition selection, SSE event payloads, AG-UI events, and request/response objects |
| `intentforge-governance` | Task orchestration, state machine, routing, strategy, coordination, scheduling, synchronous compatibility gateway, event-driven run orchestration, user-directed checkpoint transitions, and per-run runtime resolution |
| `intentforge-audit` | Run/step/tool-call records, event snapshots, replay, audit services |
| `intentforge-agent` | Agent abstraction family, routed execution contracts, and runtime integrations |
| `intentforge-channel` | Channel abstractions, runtime managers, Spring SPI bridge, and pluggable connector adapters |
| `intentforge-hook` | External hook ingress, generic and platform-specific webhook route adapters, hook-visible channel account resolution, and startup-time webhook auto-management orchestration |
| `intentforge-prompt` | Prompt definitions, registries, and pluggable prompt runtime |
| `intentforge-model` | Model catalogs, capability metadata, and pluggable model runtime |
| `intentforge-model-provider` | Model provider SPI, provider registries, and pluggable provider adapters |
| `intentforge-tool` | Tool SPI, connectors, execution, validation |
| `intentforge-memory` | Memory abstractions and implementations |
| `intentforge-config` | Config abstractions and layered providers |
| `intentforge-session` | Session lifecycle, conversation history, and pluggable session runtime |
| `intentforge-space` | Space hierarchy, inheritance resolution, and isolation bindings |
| `intentforge-boot` | Startup entrypoints for local and server modes |
| `intentforge-desktop` | Desktop host abstraction and platform adapters |

## Nested module families

### `intentforge-agent`

| Module | Role |
| --- | --- |
| `intentforge-agent-core` | `AgentTask`, `Plan`, `ContextPack`, `AgentRoute`, `AgentGateway`, `AgentRunGateway`, `AgentRunSnapshot`, `AgentRunEvent`, `AgentRunMessage`, `Artifact`, `Decision`, and executor contracts |
| `intentforge-agent-native` | Native planner, coder, reviewer implementations and default native executor factory |
| `intentforge-agent-springai` | Spring AI based chat, advisor, and tool execution adapters |
| `intentforge-agent-external` | External runtime adapters such as Codex, Gemini, AgentScope |

### `intentforge-channel`

| Module | Role |
| --- | --- |
| `intentforge-channel-core` | Channel descriptors, account/target/message/webhook models, webhook-level and message-level inbound processing contracts, routing contracts, and manager/plugin SPI |
| `intentforge-channel-local` | In-memory channel manager provider, webhook-level and message-level inbound processing pipeline, classpath SPI loading, and local plugin directory loading |
| `intentforge-channel-spring` | Spring `spring.factories` bridge for channel plugin discovery |
| `intentforge-channel-connectors` | Loopback and generic connector support entrypoints |
| `intentforge-channel-telegram` | Telegram Bot API connector implementation with driver, outbound, shared inbound normalization, polling, webhook, admin, and plugin package groups |
| `intentforge-channel-wecom` | WeCom intelligent-robot connector implementation |

### `intentforge-hook`

| Module | Role |
| --- | --- |
| `intentforge-hook` | Generic and platform-specific external hook ingress, hook-account registry, and HTTP route adapters that delegate into `ChannelInboundProcessor` |

### `intentforge-governance`

| Module | Role |
| --- | --- |
| `intentforge-governance` | Default stage-based router, per-run runtime resolver contract, and synchronous or event-driven gateways that resolve session/space/runtime bindings, emit ordered run events, pause on `awaiting_user`, expose selectable next actions, and resume or cancel the user-selected agent continuation |

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
| `intentforge-config-core` | `SpaceConfiguration`, `SpaceConfigurationStore`, `RuntimeBindings`, `RuntimeCatalog`, `ResolvedRuntimeSelection`, and config query models |
| `intentforge-config-file` | YAML/JSON local config loading and override |
| `intentforge-config-db` | Runtime and policy config persistence |
| `intentforge-config-graph` | Complex dependency and relation-driven config |

### `intentforge-space`

| Module | Role |
| --- | --- |
| `intentforge-space-core` | Company/project/product/application hierarchy definitions, resource binding profiles, SPI contracts, exceptions, and resolved profile models |
| `intentforge-space-local` | In-memory registry, multi-resource inheritance resolver, and local runtime assembly |

### `intentforge-session`

| Module | Role |
| --- | --- |
| `intentforge-session-core` | Session snapshots, message models, query contracts, not-found exception, and manager-provider SPI |
| `intentforge-session-local` | In-memory session manager, classpath provider selection, and local runtime assembly |

### `intentforge-boot`

| Module | Role |
| --- | --- |
| `intentforge-boot-local` | Local-first bootstrap, SPI runtime catalog discovery, runtime component registry assembly, space-aware per-run runtime selection, native agent gateway assembly, local exposure of both synchronous and event-driven run entrypoints, and boot-local channel inbound session persistence wiring |
| `intentforge-boot-server` | Minimal JDK `HttpServer` bootstrap, route wiring, virtual-thread-backed request execution, thin `HttpExchange` adapters, delegated hook-route registration, SSE event fan-out, terminal server entrypoints, Telegram-focused local inbound entrypoints for both long polling and webhook mode, and a WeCom robot-focused local callback entrypoint |

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
