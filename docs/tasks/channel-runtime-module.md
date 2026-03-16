# Task: Channel Runtime Module

## Requirement
Create a new `intentforge-channel` aggregate module with four submodules under the current project.
The implementation should reference the OpenClaw channel design, support pluggable multi-channel integrations,
and provide Spring SPI friendly extension points. Code comments must use English.
After the runtime spine is complete, continue by adding concrete Telegram and WeCom connector implementations
inside dedicated `intentforge-channel-telegram` and `intentforge-channel-wecom` submodules instead of keeping them
inside `intentforge-channel-connectors`.

## Acceptance Criteria
- [x] Add a new `intentforge-channel` aggregate module with exactly four submodules wired into the Maven reactor.
- [x] Provide a pluggable channel runtime spine with shared channel abstractions, manager SPI, local plugin loading, and Spring SPI bridge support.
- [x] Keep the design ready for future Telegram and WeCom adapters without hard-coding vendor logic into the core module.
- [x] Update architecture documentation to describe the new channel modules and plugin/runtime extension model.
- [x] Add builtin Telegram and WeCom connector plugins to `intentforge-channel-connectors`.
- [x] Support real outbound text delivery contracts for Telegram Bot API and WeCom application messaging with connector-specific account properties.
- [x] Cover connector descriptor exposure, request mapping, credential validation, and outbound delivery behavior with deterministic tests.
- [x] Update architecture documentation to describe the concrete Telegram and WeCom connector behavior and configuration expectations.
- [x] Pass `make test` without errors before delivery.
- [ ] Move Telegram connector code and SPI registration into `intentforge-channel-telegram`.
- [ ] Move WeCom connector code and SPI registration into `intentforge-channel-wecom`.
- [ ] Keep `intentforge-channel-connectors` focused on generic or loopback connector support after the split.
- [ ] Update Maven reactor, BOM, bootstrap dependencies, and docs to reflect the new per-channel module layout.
- [ ] Pass `make test` without errors after the module split.

## Overall Status
- status: running
- process: 20%
- current_step: 10

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | Create the task tracker, define scope, and verify git checkpoint support. | finished | commit: 6513ec0 |
| 2 | Add channel aggregate Maven structure and TDD coverage for core SPI, Spring discovery, and bootstrap integration. | finished | commit: ed74035 |
| 3 | Implement channel core/local/spring/connectors modules and runtime wiring. | finished | commit: c454ec2 |
| 4 | Update docs, run validation, and finish with checkpoint commits and final task bookkeeping. | finished | commit: 6fd8555 |
| 5 | Refresh task scope for phase two, add red tests for Telegram and WeCom connectors, and verify the expected failing state. | finished | commit: 11196e4 |
| 6 | Implement Telegram connector plugin, driver, session, and request mapping. | finished | commit: dd81ac9 |
| 7 | Implement WeCom connector plugin, driver, session, and token-aware outbound delivery support. | finished | commit: dd81ac9 |
| 8 | Update docs, run validation, and finish with checkpoint commits and final task bookkeeping for connector delivery. | finished | commit: f724f0c |
| 9 | Reopen scope for per-channel module split, add red tests and module assertions, and verify the expected failing state. | finished | commit: pending |
| 10 | Move Telegram implementation into `intentforge-channel-telegram` and update runtime wiring. | running | commit: pending |
| 11 | Move WeCom implementation into `intentforge-channel-wecom` and update runtime wiring. | notrun | commit: pending |
| 12 | Update docs, run validation, and finish with checkpoint commits and final task bookkeeping for the module split. | notrun | commit: pending |

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
| 2026-03-16 08:57:54 +0800 | running | 20% | added red tests for Telegram and WeCom connector plugins plus bootstrap exposure, then verified the expected failing state because the concrete connector classes do not exist yet |
| 2026-03-16 09:01:11 +0800 | running | 80% | implemented builtin Telegram and WeCom connector plugins, outbound session mapping, HTTP API clients, classpath SPI exposure, and verified the targeted connector plus boot-local tests passed |
| 2026-03-16 09:02:11 +0800 | running | 95% | documented connector-specific account properties and target mapping, reran full `make test` outside the sandbox, and confirmed the full Maven reactor test suite passed with the new Telegram and WeCom connectors |
| 2026-03-16 09:03:27 +0800 | finished | 100% | recorded the connector delivery checkpoint commit, completed task bookkeeping, and refreshed the Mermaid diagrams to show the concrete Telegram and WeCom outbound flow |
| 2026-03-16 09:28:40 +0800 | running | 5% | scope expanded again to split Telegram and WeCom into dedicated channel submodules; started the refactor red phase for reactor, BOM, and bootstrap wiring changes |
| 2026-03-16 09:29:55 +0800 | running | 20% | added dedicated Telegram and WeCom module skeletons plus module-local tests, then verified the expected failing state because the implementations still live in `intentforge-channel-connectors` |

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Runtime as AiAssetLocalRuntime
    participant Manager as ChannelManager
    participant Driver as Telegram/WeComChannelDriver
    participant Session as ChannelSession
    participant Vendor as Vendor HTTP API
    Runtime->>Manager: openSession(accountProfile)
    Manager->>Driver: match account type and open session
    Driver-->>Runtime: account-bound ChannelSession
    Runtime->>Session: send(ChannelOutboundRequest)
    Session->>Vendor: map target and metadata to Bot API / WeCom API
    Note over Session,Vendor: WeCom fetches and caches access token before send
    Vendor-->>Session: platform message id and accepted timestamp
    Session-->>Runtime: ChannelDeliveryResult
```

## Module Relationship Diagram

```mermaid
flowchart LR
    Core["intentforge-channel-core"] --> Local["intentforge-channel-local"]
    Core --> Spring["intentforge-channel-spring"]
    Core --> Connectors["intentforge-channel-connectors"]
    Connectors --> Telegram["telegram package"]
    Connectors --> WeCom["wecom package"]
    Spring -.discovery strategy.-> Local
    Connectors -.builtin ChannelPlugin SPI.-> Local
    Plugins["plugins/*.jar"] --> Local
    Local --> Boot["intentforge-boot-local"]
    Connectors --> Boot
    Boot --> Runtime["AiAssetLocalRuntime / RuntimeCatalog"]
```
