# Task: WeCom Robot Server Main

## Requirement
Add a WeCom intelligent-robot-focused boot-server entrypoint and local smoke-test documentation.
The new entrypoint should manually register one WeCom robot hook account from system properties or environment variables so local callback testing does not require a custom bootstrap class.

## Acceptance Criteria
- [x] `intentforge-boot-server` provides one WeCom robot-specific terminal main entrypoint.
- [x] The entrypoint resolves WeCom robot settings from system properties first and environment variables second.
- [x] The entrypoint registers one hook-visible WeCom robot account and exposes the canonical WeCom callback route.
- [x] Tests cover settings resolution, invalid required settings, and hook-route availability.
- [ ] Boot-server README documents a copy-paste local WeCom robot smoke-test flow.
- [ ] `make test` passes after the new entrypoint is added.

## Overall Status
- status: running
- process: 60%
- current_step: 3

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | Add task scope and red tests for WeCom robot server settings and hook registration. | finished | commit: pending |
| 2 | Implement the WeCom robot server main and settings mapping in boot-server. | finished | commit: pending |
| 3 | Document the local WeCom robot smoke-test flow and update architecture notes. | running | commit: pending |
| 4 | Run full validation, finalize bookkeeping, and close the task. | notrun | commit: pending |

## Update Log
| time | status | process | update |
| --- | --- | --- | --- |
| 2026-03-18 15:35:00 +0800 | running | 5% | Initialized task for adding a WeCom intelligent-robot boot-server entrypoint and local smoke-test documentation. |
| 2026-03-18 15:52:00 +0800 | running | 60% | Added `WeComRobotServerMain` and `WeComRobotServerSettings`, and the targeted `WeComRobotServerMainTest` now passes. |
