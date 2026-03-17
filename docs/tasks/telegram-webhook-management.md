# Task: Telegram Webhook Management

## Requirement
Add automatic Telegram webhook lifecycle management for manually registered hook accounts. The server should be able to reconcile Telegram webhook state at startup without introducing a formal configuration center yet.

## Acceptance Criteria
- [ ] Telegram connector exposes account-bound webhook administration for `setWebhook`, `deleteWebhook`, and `getWebhookInfo`.
- [ ] Server bootstrap automatically reconciles manually registered Telegram webhook accounts when automatic management is enabled in account properties.
- [ ] Hook account registration remains manual for this phase, but automatic webhook management can derive the public webhook URL from account properties or the running server base URI.
- [ ] Documentation describes the new manual-account webhook management flow.
- [ ] Validation covers unit and integration cases and `make test` passes.

## Overall Status
- status: running
- process: 5%
- current_step: 1

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | Define webhook management task scope and add red tests for Telegram admin lifecycle and startup reconciliation | running | commit: pending |
| 2 | Implement Telegram webhook administration and generic webhook auto-management wiring | notrun | commit: pending |
| 3 | Update docs, run verification, and finalize checkpoints | notrun | commit: pending |

## Update Log
| time | status | process | update |
| --- | --- | --- | --- |
| 2026-03-17 10:15:07 +0800 | running | 5% | Initialized task for Telegram automatic webhook lifecycle management on top of manual hook account registration. |
