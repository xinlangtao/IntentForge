# Task: Channel Runtime Module

## Requirement
Create a new `intentforge-channel` aggregate module with four submodules under the current project.
The implementation should reference the OpenClaw channel design, support pluggable multi-channel integrations,
and provide Spring SPI friendly extension points. Code comments must use English.
After the runtime spine is complete, continue by adding concrete Telegram and WeCom connector implementations
inside `intentforge-channel-connectors`.

## Acceptance Criteria
- [x] Add a new `intentforge-channel` aggregate module with exactly four submodules wired into the Maven reactor.
- [x] Provide a pluggable channel runtime spine with shared channel abstractions, manager SPI, local plugin loading, and Spring SPI bridge support.
- [x] Keep the design ready for future Telegram and WeCom adapters without hard-coding vendor logic into the core module.
- [x] Update architecture documentation to describe the new channel modules and plugin/runtime extension model.
- [ ] Add builtin Telegram and WeCom connector plugins to `intentforge-channel-connectors`.
- [ ] Support real outbound text delivery contracts for Telegram Bot API and WeCom application messaging with connector-specific account properties.
- [ ] Cover connector descriptor exposure, request mapping, credential validation, and outbound delivery behavior with deterministic tests.
- [ ] Update architecture documentation to describe the concrete Telegram and WeCom connector behavior and configuration expectations.
- [ ] Pass `make test` without errors before delivery.

## Overall Status
- status: running
- process: 5%
- current_step: 5

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | Create the task tracker, define scope, and verify git checkpoint support. | finished | commit: 6513ec0 |
| 2 | Add channel aggregate Maven structure and TDD coverage for core SPI, Spring discovery, and bootstrap integration. | finished | commit: ed74035 |
| 3 | Implement channel core/local/spring/connectors modules and runtime wiring. | finished | commit: c454ec2 |
| 4 | Update docs, run validation, and finish with checkpoint commits and final task bookkeeping. | finished | commit: 6fd8555 |
| 5 | Refresh task scope for phase two, add red tests for Telegram and WeCom connectors, and verify the expected failing state. | running | commit: pending |
| 6 | Implement Telegram connector plugin, driver, session, and request mapping. | notrun | commit: pending |
| 7 | Implement WeCom connector plugin, driver, session, and token-aware outbound delivery support. | notrun | commit: pending |
| 8 | Update docs, run validation, and finish with checkpoint commits and final task bookkeeping for connector delivery. | notrun | commit: pending |

## Update Log
| time | status | process | update |
| --- | --- | --- | --- |
| 2026-03-13 15:18:19 +0800 | running | 5% | task initialized, git repository availability verified, and channel module development started |
| 2026-03-13 15:23:39 +0800 | running | 20% | added the channel aggregate Maven structure and TDD test skeleton, then confirmed the expected red test state due to missing production channel classes |
| 2026-03-13 15:29:56 +0800 | running | 70% | implemented channel core/local/spring/connectors modules, wired channel manager into local bootstrap, and verified module plus boot-local targeted tests; one broader sandbox run still hit an unrelated socket-permission failure in existing tool connector tests |
| 2026-03-13 15:29:56 +0800 | running | 85% | updated architecture and README documents to describe the new channel runtime modules and their plugin discovery model; full validation remains pending |
| 2026-03-13 15:41:32 +0800 | running | 95% | reran `make test` outside the sandbox, synchronized boot-local and boot-server runtime-selection assertions with the new `CHANNEL_MANAGER` capability, and confirmed the full Maven reactor test suite passed |
| 2026-03-13 15:45:25 +0800 | finished | 100% | recorded the final checkpoint commit, completed task bookkeeping, and documented the bootstrap plus plugin-discovery flow with Mermaid diagrams |
| 2026-03-16 08:55:31 +0800 | running | 5% | scope expanded to phase two connector delivery; reopened the task, added Telegram and WeCom connector acceptance criteria, and started the red-test phase |

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Bootstrap as AiAssetLocalBootstrap
    participant Provider as ChannelManagerProvider
    participant Manager as InMemoryChannelManager
    participant Discovery as ChannelPluginDiscoveryStrategy
    participant Plugin as ChannelPlugin
    participant Directory as DirectoryChannelPluginManager
    participant Catalog as RuntimeCatalog
    participant Runtime as AiAssetLocalRuntime
    Bootstrap->>Provider: load via ServiceLoader
    Provider-->>Bootstrap: create manager
    Bootstrap->>Discovery: discover classpath plugins
    Discovery-->>Bootstrap: ChannelPlugin instances
    Bootstrap->>Plugin: collect ChannelDriver descriptors
    Bootstrap->>Manager: register drivers
    Bootstrap->>Directory: sync plugins/ channel jars
    Directory-->>Bootstrap: external ChannelPlugin instances
    Bootstrap->>Manager: register external drivers
    Bootstrap->>Catalog: publish CHANNEL_MANAGER implementation
    Bootstrap-->>Runtime: expose default ChannelManager
```

## Module Relationship Diagram

```mermaid
flowchart LR
    Core["intentforge-channel-core"] --> Local["intentforge-channel-local"]
    Core --> Spring["intentforge-channel-spring"]
    Core --> Connectors["intentforge-channel-connectors"]
    Spring -.discovery strategy.-> Local
    Connectors -.built-in plugins.-> Local
    Plugins["plugins/*.jar"] --> Local
    Local --> Boot["intentforge-boot-local"]
    Connectors --> Boot
    Boot --> Runtime["AiAssetLocalRuntime / RuntimeCatalog"]
```
