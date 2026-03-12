# Task: Session Module

## Requirement
参考仓库内其他模块的 SPI 机制，新增 `session` 模块，实现会话管理能力。模块设计需遵循现有分层习惯，至少提供 `core + local` 两层：`core` 承载会话领域模型、SPI、契约与异常，`local` 承载本地内存实现与基于 SPI 的装配。实现需与现有插件运行时风格保持一致，并补充测试与文档。

## Acceptance Criteria
- [x] 新增 `intentforge-session` 聚合模块，并完成 `intentforge-session-core` 与 `intentforge-session-local` 分层。
- [x] `session-core` 提供清晰的会话管理领域模型、查询/写入契约、SPI Provider 接口及必要异常。
- [x] `session-local` 提供内存版会话管理实现、classpath SPI 选择逻辑，以及覆盖正常、边界、非法输入、异常场景的测试。
- [x] 根 POM、模块文档与架构文档完成更新，且 `make test` 通过。
- [x] 任务文件持续记录 checkpoint，并与实际 git commit 保持一致。

## Overall Status
- status: finished
- process: 100%
- current_step: completed

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | 阅读现有 SPI 模块实现并明确 session 模块边界 | finished | commit: 77227d9 |
| 2 | 先编写 session 核心行为与 SPI 选择测试基线 | finished | commit: 915bd1f |
| 3 | 完成 session core/local 实现与模块接入 | finished | commit: 1059e1f |
| 4 | 更新文档并执行完整验证与收尾 | finished | commit: e456414 |

## Update Log
| time | status | process | update |
| --- | --- | --- | --- |
| 2026-03-12 15:18:00 +0800 | notrun | 0% | task initialized |
| 2026-03-12 15:20:00 +0800 | running | 5% | task started, git checkpoint capability verified, analyzing SPI references for session module |
| 2026-03-12 15:28:00 +0800 | running | 18% | session scope finalized: core/local split, provider SPI selection, in-memory session management, and boot-local integration |
| 2026-03-12 17:35:00 +0800 | running | 35% | failing test baseline added for session domain models, local SPI selection, and boot-local integration |
| 2026-03-12 17:38:00 +0800 | running | 75% | session core/local implementation completed, ServiceLoader provider selection added, and targeted Maven tests passed |
| 2026-03-12 17:39:00 +0800 | finished | 100% | architecture docs and README updated, full `make test` passed, final checkpoint commit pending write-back |
| 2026-03-12 17:40:00 +0800 | finished | 100% | final bookkeeping commit recorded step 4 checkpoint `e456414` |
