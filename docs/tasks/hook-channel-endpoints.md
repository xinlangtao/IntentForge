# Task: Hook Channel Endpoints

## Requirement
Add concrete Telegram and WeCom hook endpoint families inside `intentforge-hook`.
These platform-specific HTTP paths should live beside the existing generic channel hook route and delegate into the same `ChannelInboundProcessor` pipeline.

## Acceptance Criteria
- [ ] Expose one Telegram-specific hook route under `/open-api/hooks/telegram/accounts/{accountId}/webhook`.
- [ ] Expose one WeCom-specific hook route under `/open-api/hooks/wecom/accounts/{accountId}/callback`.
- [ ] Keep platform-specific hook routes implemented inside `intentforge-hook` instead of `intentforge-boot-server` or connector modules.
- [ ] Reuse the existing transport-neutral `ChannelWebhookEndpointController` and `HookAccountRegistry` instead of duplicating connector logic.
- [ ] Wire the new endpoint families through `HookHttpRouteRegistrar` and `AiAssetServerBootstrap`.
- [ ] Cover Telegram route parsing, WeCom route parsing, unsupported path handling, and boot-server integration with deterministic tests.
- [ ] Update architecture and API documentation for the new platform-specific hook paths.
- [ ] Pass `make test` without errors after the new endpoint families are added.

## Overall Status
- status: running
- process: 20%
- current_step: 2

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | Create the task tracker, add red tests for Telegram and WeCom hook endpoint families, and verify the expected failing state. | finished | commit: pending |
| 2 | Implement Telegram and WeCom hook HTTP handlers plus reusable registration inside `intentforge-hook`. | running | commit: pending |
| 3 | Wire the new endpoint families through `boot-server` and verify end-to-end hook routing. | notrun | commit: pending |
| 4 | Update docs and API spec, rerun validation, and finish with checkpoint commits plus final bookkeeping. | notrun | commit: pending |

## Update Log
| time | status | process | update |
| --- | --- | --- | --- |
| 2026-03-17 00:00:00 +0800 | running | 5% | task initialized for concrete Telegram and WeCom hook endpoint families inside `intentforge-hook`; scope fixed to platform-specific HTTP routes that still delegate into the shared channel inbound pipeline |
| 2026-03-17 09:18:02 +0800 | running | 20% | added red tests for Telegram-specific and WeCom-specific hook routes plus boot-server integration, then confirmed the expected failing state because the new platform-specific HTTP handler classes do not exist yet |
