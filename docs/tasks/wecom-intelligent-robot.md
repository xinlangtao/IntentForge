# Task: WeCom Intelligent Robot

## Requirement
Replace the existing WeCom application-messaging connector with a WeCom intelligent-robot connector.
The `intentforge-channel-wecom` module should only support the intelligent-robot callback model and its matching outbound message sending shape, while keeping the existing `channel`, `hook`, and `boot` module boundaries unchanged.

## Acceptance Criteria
- [ ] The legacy WeCom application-messaging implementation is removed from `intentforge-channel-wecom`.
- [ ] The WeCom connector is reorganized into package groups for plugin, driver, inbound, outbound, crypto, and shared support.
- [ ] Inbound WeCom intelligent-robot callbacks support verification, signature validation, AES decryption, and normalized text-message output.
- [ ] Outbound WeCom intelligent-robot sending maps `ChannelOutboundRequest` into one robot-oriented send command.
- [ ] The existing WeCom hook route `/open-api/hooks/wecom/accounts/{accountId}/callback` continues to work with the new connector.
- [ ] Documentation reflects that `intentforge-channel-wecom` now targets intelligent robots instead of self-built applications.
- [ ] Full `make test` passes after the replacement.

## Overall Status
- status: running
- process: 5%
- current_step: 1

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | Add task scope and red tests for the intelligent-robot crypto, inbound callback, and outbound session behavior. | running | commit: pending |
| 2 | Replace the WeCom connector implementation with intelligent-robot driver, crypto, inbound, and outbound packages. | notrun | commit: pending |
| 3 | Update hook-facing tests, docs, and runtime notes for the intelligent-robot-only WeCom connector. | notrun | commit: pending |
| 4 | Run full validation, finalize bookkeeping, and close the task. | notrun | commit: pending |

## Update Log
| time | status | process | update |
| --- | --- | --- | --- |
| 2026-03-18 16:20:00 +0800 | running | 5% | Initialized task for replacing the legacy WeCom application connector with an intelligent-robot-only connector. |
