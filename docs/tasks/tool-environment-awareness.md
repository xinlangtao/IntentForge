# Task: Tool Environment Awareness

## Requirement
Add environment awareness to the tool runtime so startup and tool execution can know the current operating system, shell/terminal, and IDE entry information through a standard context and a readable tool.

## Acceptance Criteria
- [x] `ToolExecutionContext` exposes a normalized environment model available immediately after startup context creation.
- [x] Tool runtime provides a read-only tool to inspect current environment details including OS, shell/terminal, and IDE entry candidates.
- [x] Boot/tool integration tests and unit tests cover normal, boundary, invalid, and fallback cases for environment awareness.
- [x] `make test` passes without introducing warnings or errors.

## Overall Status
- status: finished
- process: 100%
- current_step: completed

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | Create progress file, inspect docs/runtime, and confirm environment-awareness design scope | finished | commit: df66374 |
| 2 | Add failing tests for context environment capture and read-only environment tool | finished | commit: c8d2a53 |
| 3 | Implement normalized environment model, context injection, and environment tool/plugin wiring | finished | commit: c135435 |
| 4 | Run full verification, update progress file, and finalize git checkpoints | finished | commit: c135435 |

## Update Log
| time | status | process | update |
| --- | --- | --- | --- |
| 2026-03-12 14:32:04 +0800 | notrun | 0% | task initialized |
| 2026-03-12 14:32:04 +0800 | running | 5% | docs and startup/tool runtime inspected; confirmed git workspace is available and environment awareness is missing from both bootstrap and tool context |
| 2026-03-12 14:35:35 +0800 | running | 25% | added red tests for runtime environment detection, context exposure, connector tool registration, and boot integration |
| 2026-03-12 14:41:07 +0800 | running | 75% | implemented runtime environment model/detector, injected it into tool context, added a read-only environment tool, and aligned shell/validation command startup with detected shell |
| 2026-03-12 14:41:45 +0800 | finished | 100% | full `make test` passed for the entire reactor; task bookkeeping finalized |
