# Task: Space Isolation Module

## Requirement
新增 space 空间隔离模块，支持公司空间、项目空间、产品空间与应用空间。每个空间都可配置 skill、tool、model、model-provider、config、memory 等资源；解析时遵循 `application > product > project > company` 优先级，并在未覆盖时继承父空间配置。
在此基础上调整模块设计：`intentforge-space` 不能只保留一个 `intentforge-space-core` 子模块，需要按仓库分层习惯拆分为至少 `core + local` 两层。`core` 仅保留领域模型、SPI、异常与通用契约；`local` 承载 `InMemorySpaceRegistry`、`DefaultSpaceResolver` 和本地运行时装配实现，`boot-local` 依赖也需要同步收敛到合理模块。
继续按系统真实资源形态优化设计：`SpaceProfile` 里 `model / model-provider / memory` 不能再用单个 `String`，需要改成一对多集合；同时补充 `agent`、`prompt` 等系统已有资源绑定能力。最终 space 资源模型应至少覆盖 `skillIds`、`agentIds`、`promptIds`、`toolIds`、`modelIds`、`modelProviderIds`、`memoryIds` 以及 `config`，并保持分层继承解析能力。

## Acceptance Criteria
- [x] `intentforge-space` 已拆分为 `intentforge-space-core` 与 `intentforge-space-local`，职责边界清晰。
- [x] `SpaceProfile` 与 `ResolvedSpaceProfile` 使用集合型资源绑定，覆盖 `skillIds`、`agentIds`、`promptIds`、`toolIds`、`modelIds`、`modelProviderIds`、`memoryIds` 与 `config`。
- [x] 资源解析仍按 `application > product > project > company` 继承链生效，并覆盖正常、继承、边界、非法输入与异常场景测试。
- [x] `boot-local` 与相关文档基于新资源模型更新完成，`make test` 重新通过。
- [x] 任务文件记录本轮资源模型重构 checkpoint，并与最新范围保持一致。

## Overall Status
- status: finished
- process: 100%
- current_step: completed

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | 分析现有模块与确定 space 接入点 | finished | commit: 0309905 |
| 2 | 先编写单模块 space 继承与优先级解析测试基线 | finished | commit: b85db2e |
| 3 | 完成单模块 baseline 实现、boot 接入与初次收尾 | finished | commits: e62f31e, 3932e92, 5151551 |
| 4 | 按新需求重定义 space 模块边界与任务范围 | finished | commit: 56af835 |
| 5 | 拆分 `core/local` 模块并迁移实现与依赖 | finished | commit: 60ef071 |
| 6 | 修正文档、迁移测试并重新执行 `make test` | finished | commits: 60ef071, fe89f30 |
| 7 | 按系统资源重新定义 space 资源模型与验收范围 | finished | commit: 1a5e7cf |
| 8 | 将 space 资源模型改为集合绑定并迁移解析与测试 | finished | commit: e61e6cf |
| 9 | 更新文档、执行 `make test` 并完成最终回写 | finished | commit: e61e6cf |

## Update Log
| time | status | process | update |
| --- | --- | --- | --- |
| 2026-03-12 13:41:53 +0800 | notrun | 0% | task initialized |
| 2026-03-12 13:41:53 +0800 | running | 5% | task started, git checkpoint capability verified |
| 2026-03-12 13:44:43 +0800 | running | 25% | module entry and failing tests added for space hierarchy resolution |
| 2026-03-12 13:47:03 +0800 | running | 55% | space core implementation completed and targeted tests passed |
| 2026-03-12 13:49:09 +0800 | finished | 100% | boot integration, docs, and full `make test` completed successfully |
| 2026-03-12 14:24:06 +0800 | running | 70% | scope changed: single `intentforge-space-core` is no longer sufficient, task reopened for multi-module split |
| 2026-03-12 14:24:06 +0800 | running | 75% | requirement and acceptance criteria refreshed for multi-module `core + local` split |
| 2026-03-12 14:24:06 +0800 | running | 75% | checkpoint recorded for scope refresh, next step is module split execution |
| 2026-03-12 14:24:06 +0800 | running | 80% | step 5 started, preparing module split and implementation migration |
| 2026-03-12 14:30:12 +0800 | running | 90% | `space-local` module created, implementations and targeted tests migrated, boot-local dependency switched |
| 2026-03-12 14:30:34 +0800 | finished | 100% | module split completed, docs refreshed, and full `make test` passed |
| 2026-03-12 14:30:34 +0800 | finished | 100% | final bookkeeping checkpoint persisted for step 6 commit write-back |
| 2026-03-12 15:00:42 +0800 | running | 82% | scope changed again: resource model must align with system assets and support multi-valued bindings |
| 2026-03-12 15:10:51 +0800 | running | 92% | collection-based space resource model, resolver migration, targeted tests, and docs refresh completed; final checkpoint and full `make test` pending |
| 2026-03-12 15:11:38 +0800 | finished | 100% | full `make test` passed, checkpoint `e61e6cf` verified, and task bookkeeping synchronized |
