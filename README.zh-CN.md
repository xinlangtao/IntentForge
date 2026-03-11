# IntentForge · 意铸

[English](README.md) | [简体中文](README.zh-CN.md)

> Think it. Forge it.  
> 意之所至，万物可构。

IntentForge 是一个面向 AI 时代的 intent-driven development framework。

描述你的意图。  
IntentForge 会把它锻造成真实系统。

没有样板代码。  
没有沉重配置。  
只有 **intent → creation**。

## Slogan

Think it. Forge it.  
意之所至，万物可构。

备用：

- From intent to reality.
- Build what you imagine.
- Intent → Creation.

## 当前状态

- 版本基线：`nightly-SNAPSHOT`
- Java 基线：`JDK 25`
- 统一 BOM：`Spring Boot 3.5.11` + `Spring AI 1.1.1`
- 当前阶段：仓库骨架初始化

## 模块图

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

## 架构概览

- `governance` 负责管
- `agent` 负责想
- `tool` 负责做
- `memory/config` 负责提供上下文
- `audit` 负责留痕
- `boot` 负责启动
- `desktop` 负责桌面端宿主与平台适配

更多说明见 [docs/architecture/module-map.md](docs/architecture/module-map.md)。

## 构建

```bash
./mvnw -B -ntp validate
./mvnw -B -ntp flatten:flatten -DskipTests
```

## Make 快捷命令

```bash
make help
make help LOCALE=zh
LANG=zh_CN.UTF-8 make help
make validate
make package SKIPTESTS=true
make install MODEL=intentforge-api
```

## 目录说明

- `frontend/desktop` 预留给 Electron + Vue 渲染层。
- `intentforge-desktop-*` 预留给 JVM 侧桌面宿主与平台适配。
- `python/runtime` 和 `scripts` 目前都是占位目录。
