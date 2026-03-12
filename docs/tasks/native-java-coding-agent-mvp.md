# Task: Native Java Coding Agent MVP

## Requirement
Implement an MVP native Java coding agent that connects the existing session, space, prompt, model, provider, tool, governance, and boot modules. The design must include an agent gateway and routing mechanism so the runtime can resolve context and dispatch work across planner/coder/reviewer style native agents.

## Acceptance Criteria
- [ ] `intentforge-agent-core` defines the public agent task/context/decision/gateway contracts needed to execute a coding request across module boundaries.
- [ ] `intentforge-governance` provides MVP routing/orchestration that can choose native agent stages and preserve route information for execution.
- [ ] `intentforge-agent-native` provides a working native Java coding agent flow that resolves session/space/tool context and produces deterministic execution output for MVP scenarios.
- [ ] `intentforge-boot-local` exposes the wired agent runtime so local bootstrap can execute the MVP flow end-to-end.
- [ ] Unit and integration tests cover normal, boundary, invalid input, and exception paths, and `make test` passes without warnings or errors.

## Overall Status
- status: running
- process: 25%
- current_step: 2

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | Create progress file, inspect architecture/spec docs, inspect existing agent/governance/boot runtime, and verify git checkpoint capability | finished | commit: bd46a0c |
| 2 | Add failing tests for agent contracts, routing/orchestration, native execution, and local bootstrap integration | running | commit: pending |
| 3 | Implement agent core abstractions, governance router/gateway, native MVP execution flow, and boot wiring | notrun | commit: pending |
| 4 | Update docs, run full verification, sync task bookkeeping, and finalize checkpoints | notrun | commit: pending |

## Update Log
| time | status | process | update |
| --- | --- | --- | --- |
| 2026-03-12 18:29:17 +0800 | notrun | 0% | task initialized |
| 2026-03-12 18:29:17 +0800 | running | 5% | task started; required docs inspected; current agent/governance modules are mostly placeholders; git checkpoint capability verified |
| 2026-03-12 18:33:43 +0800 | running | 25% | finalized MVP layering: agent-core for contracts, governance for routing/gateway, agent-native for planner/coder/reviewer, boot-local for wiring; started adding red tests and module dependencies |
