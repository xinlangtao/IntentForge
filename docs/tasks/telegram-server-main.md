# Task: Telegram Server Main

## Requirement
Add a Telegram-specific boot-server entrypoint that manually registers one Telegram hook account from system properties or environment variables so local end-to-end webhook testing does not require a custom bootstrap class.

## Acceptance Criteria
- [ ] `intentforge-boot-server` provides a Telegram-specific main class for local server startup.
- [ ] The Telegram-specific main can resolve account settings from system properties with environment-variable fallback.
- [ ] The Telegram-specific main manually registers the Telegram hook account and exposes the Telegram webhook route.
- [ ] Documentation describes the new Telegram-specific startup entrypoint and its supported settings.
- [ ] Validation covers settings resolution, invalid input, integration behavior, and full `make test`.

## Overall Status
- status: running
- process: 5%
- current_step: 1

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | Add task scope and red tests for Telegram-specific server main settings and startup behavior | running | commit: pending |
| 2 | Implement Telegram-specific server main and manual hook account registration wiring | notrun | commit: pending |
| 3 | Update docs, run verification, and finalize checkpoints | notrun | commit: pending |

## Update Log
| time | status | process | update |
| --- | --- | --- | --- |
| 2026-03-17 10:33:57 +0800 | running | 5% | Initialized task for a Telegram-specific boot-server entrypoint that manually registers one hook account from properties or environment variables. |
