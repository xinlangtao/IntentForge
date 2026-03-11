# IntentForge

[English](README.md) | [简体中文](README.zh-CN.md)

> Think it. Forge it.  
> From intent to creation.

IntentForge is an intent-driven development framework for the AI era.

Describe what you want.  
IntentForge will forge it into real systems.

No boilerplate.  
No heavy setup.  
Just **intent → creation**.

## Slogan

Think it. Forge it.  
From intent to creation.

Alternatives:

- From intent to reality.
- Build what you imagine.
- Intent → Creation.

## Status

- Version baseline: `nightly-SNAPSHOT`
- Java baseline: `JDK 25`
- Managed platform BOM: `Spring Boot 3.5.11` + `Spring AI 1.1.1`
- Current stage: repository skeleton initialization

## Module Map

```text
intentforge/
├─ intentforge-bom
├─ intentforge-common
├─ intentforge-api
├─ intentforge-governance
├─ intentforge-audit
├─ intentforge-agent
│  ├─ intentforge-agent-core
│  ├─ intentforge-agent-native
│  ├─ intentforge-agent-springai
│  └─ intentforge-agent-external
├─ intentforge-tool
│  ├─ intentforge-tool-core
│  ├─ intentforge-tool-mcp
│  ├─ intentforge-tool-connectors
│  ├─ intentforge-tool-fs
│  ├─ intentforge-tool-shell
│  └─ intentforge-tool-validation
├─ intentforge-memory
│  ├─ intentforge-memory-core
│  ├─ intentforge-memory-file
│  ├─ intentforge-memory-sql
│  └─ intentforge-memory-graph
├─ intentforge-config
│  ├─ intentforge-config-core
│  ├─ intentforge-config-file
│  ├─ intentforge-config-db
│  └─ intentforge-config-graph
├─ intentforge-boot
│  ├─ intentforge-boot-local
│  └─ intentforge-boot-server
├─ intentforge-desktop
│  ├─ intentforge-desktop-core
│  ├─ intentforge-desktop-windows
│  ├─ intentforge-desktop-macos
│  └─ intentforge-desktop-linux
├─ frontend
│  ├─ desktop
│  └─ ui
├─ python
│  └─ runtime
└─ scripts
```

## Architecture Summary

- `governance` orchestrates
- `agent` thinks and decides
- `tool` executes
- `memory/config` provides context
- `audit` records and replays
- `boot` starts runtime modes
- `desktop` hosts desktop runtime and platform adapters

More detail: [docs/architecture/module-map.md](docs/architecture/module-map.md)

## Build

```bash
./mvnw -B -ntp validate
./mvnw -B -ntp flatten:flatten -DskipTests
```

## Make Shortcuts

```bash
make help
make help LOCALE=zh
LANG=zh_CN.UTF-8 make help
make validate
make package SKIPTESTS=true
make install MODEL=intentforge-api
```

See [README.zh-CN.md](README.zh-CN.md) for the Chinese version.

## Repository Layout Notes

- `frontend/desktop` is reserved for the Electron + Vue rendering layer.
- `intentforge-desktop-*` is reserved for JVM-side desktop host and platform adapters.
- `python/runtime` and `scripts` are placeholders for future runtime tooling and automation.
