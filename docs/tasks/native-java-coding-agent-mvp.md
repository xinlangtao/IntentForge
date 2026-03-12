# Task: Native Java Coding Agent Event-Driven Run Model

## Requirement
Starting from the completed event-driven native Java coding agent runtime, extend the MVP with the minimum API and boot-server chain required to run the agent from a terminal through a real server entrypoint. The next phase must preserve the existing run lifecycle while adding transport contracts, server bootstrap wiring, and a minimal HTTP/SSE flow that can create a run, observe run events, send user feedback, and cancel the run. API request handling threads should prefer virtual threads unless a concrete blocker is discovered and recorded.

## Acceptance Criteria
- [x] `intentforge-agent-core` defines run/event/lifecycle contracts for incremental execution, user-feedback checkpoints, and resume/cancel semantics across module boundaries.
- [x] `intentforge-governance` provides an event-driven run orchestrator that emits ordered stage events, supports `awaiting_user` pauses, and can resume planner/coder/reviewer execution with new feedback.
- [x] `intentforge-agent-native` adapts planner/coder/reviewer execution to the new run lifecycle and preserves multi-turn context across turns.
- [x] `intentforge-boot-local` exposes the event-driven runtime entry needed to start, observe, and continue a run without requiring a future API transport first.
- [x] Unit and integration tests cover normal, boundary, invalid input, pause/resume, cancel, and exception paths, and `make test` passes without warnings or errors.
- [x] `intentforge-api` defines the minimum HTTP contract for run creation, event subscription, user feedback resume, and cancel operations, and the contract documentation is synchronized.
- [x] `intentforge-boot-server` provides the minimum runnable server entrypoint that wires the existing `AgentRunGateway` into HTTP and SSE transport without duplicating governance logic, and request handling prefers virtual threads.
- [x] A local terminal smoke path can start the server and drive one real run through HTTP/SSE end to end.

## Overall Status
- status: finished
- process: 100%
- current_step: completed

## Steps
| step | description | status | note |
| --- | --- | --- | --- |
| 1 | Create progress file, inspect architecture/spec docs, inspect existing agent/governance/boot runtime, and verify git checkpoint capability | finished | commit: bd46a0c |
| 2 | Add failing tests for agent contracts, routing/orchestration, native execution, and local bootstrap integration | finished | commit: 7fd06db |
| 3 | Implement agent core abstractions, governance router/gateway, native MVP execution flow, and boot wiring | finished | commit: c1f72dd |
| 4 | Update docs, run full verification, sync task bookkeeping, and finalize synchronous MVP checkpoints | finished | commits: 471b451, 29ce8e5 |
| 5 | Re-scope the completed synchronous MVP to an event-driven multi-turn run model and preserve the recovery baseline | finished | commit: 2404562 |
| 6 | Add failing tests for run lifecycle, event emission, awaiting-user pause, resume, cancel, and transport-agnostic observation flow | finished | commit: ad2ee3c |
| 7 | Implement agent-core run/event contracts, governance orchestrator, native feedback loop, and boot-local event-driven wiring | finished | commit: e81d513 |
| 8 | Update docs, run full verification, sync task bookkeeping, and finalize event-driven checkpoints | finished | commits: e81d513, 87e6526 |
| 9 | Re-scope the completed event-driven runtime to include the minimum API and boot-server chain and preserve the recovery baseline | finished | commits: 3d6a9e8, 1428797 |
| 10 | Add failing tests and API contract updates for run create, SSE events, feedback resume, cancel, minimal boot-server startup flow, and preferred virtual-thread request handling | finished | commit: 8003fe9 |
| 11 | Implement intentforge-api transport contracts and boot-server HTTP/SSE wiring on top of `AgentRunGateway`, preferring virtual threads for request processing | finished | commit: e6495f1 |
| 12 | Update docs, verify terminal smoke flow and full test suite, sync task bookkeeping, and finalize API/server checkpoints | finished | commit: fd7e884 |

## Update Log
| time | status | process | update |
| --- | --- | --- | --- |
| 2026-03-12 18:29:17 +0800 | notrun | 0% | task initialized |
| 2026-03-12 18:29:17 +0800 | running | 5% | task started; required docs inspected; current agent/governance modules are mostly placeholders; git checkpoint capability verified |
| 2026-03-12 18:33:43 +0800 | running | 25% | finalized MVP layering: agent-core for contracts, governance for routing/gateway, agent-native for planner/coder/reviewer, boot-local for wiring; started adding red tests and module dependencies |
| 2026-03-12 18:42:08 +0800 | running | 75% | implemented agent-core contracts, governance routing/gateway, native planner/coder/reviewer flow, and boot-local wiring; targeted reactor tests passed for the affected modules |
| 2026-03-12 18:43:02 +0800 | running | 90% | updated architecture docs for the new runtime spine and verified the full reactor with `make test` |
| 2026-03-12 18:43:21 +0800 | finished | 100% | task bookkeeping synchronized after docs checkpoint `471b451`; all acceptance criteria satisfied and full `make test` already passed |
| 2026-03-12 20:46:24 +0800 | running | 10% | scope changed: reopen task from synchronous batch MVP to event-driven multi-turn run model; recovery baseline is the completed checkpoint chain ending at `29ce8e5` |
| 2026-03-12 20:47:29 +0800 | running | 10% | scope-change checkpoint `2404562` recorded; next execution step is to add red tests for the event-driven run lifecycle |
| 2026-03-12 20:49:11 +0800 | running | 20% | step 6 started; current design has no run/event/observer contracts and no pause-resume lifecycle, so the next checkpoint will add red tests around event-driven orchestration and multi-turn feedback |
| 2026-03-12 20:53:47 +0800 | running | 35% | added red tests for agent-core run messages, governance run lifecycle, native feedback propagation, and boot-local event-driven integration; targeted build fails at missing run/event contracts as expected |
| 2026-03-12 20:58:36 +0800 | running | 80% | implemented event-driven run contracts, in-memory run orchestration, native feedback propagation, and boot-local wiring; targeted reactor tests passed for agent-core, governance, agent-native, and boot-local |
| 2026-03-12 21:00:36 +0800 | finished | 100% | full `make test` passed after the event-driven run model landed; acceptance criteria are satisfied and final task bookkeeping is being synchronized |
| 2026-03-12 21:01:21 +0800 | finished | 100% | task bookkeeping synchronized after docs checkpoint `87e6526`; event-driven multi-turn run model is fully closed |
| 2026-03-12 21:44:12 +0800 | running | 10% | scope changed again: reopen the finished event-driven runtime task to add the minimum API and boot-server chain needed for terminal real calls; current gap is that `boot-server` is still a placeholder and there is no HTTP/SSE transport layer yet |
| 2026-03-12 21:44:33 +0800 | running | 15% | scope-change checkpoint `3d6a9e8` recorded; next execution step is to add red tests and minimal API contracts for the server chain |
| 2026-03-12 21:51:03 +0800 | running | 15% | scope refined: the API/server phase must prefer virtual threads for request handling unless a concrete blocker is found and recorded during implementation |
| 2026-03-12 21:51:18 +0800 | running | 15% | scope refinement checkpoint `1428797` recorded; step 10 remains the next active work item with virtual-thread preference now fixed in the task definition |
| 2026-03-12 21:57:35 +0800 | running | 20% | step 10 started; current repo has no `docs/api-spec.yaml`, no HTTP DTOs in `intentforge-api`, and no runnable `boot-server`, so the next checkpoint will add red tests plus the first minimal OpenAPI contract for create, events, feedback, cancel, and virtual-thread-backed server startup |
| 2026-03-12 22:00:33 +0800 | running | 35% | added red tests for HTTP DTOs, boot-server HTTP/SSE lifecycle, cancel, and virtual-thread executor preference, and created the first `docs/api-spec.yaml`; targeted build fails at missing API transport models and server bootstrap classes as expected |
| 2026-03-12 22:04:27 +0800 | running | 70% | implemented minimal HTTP DTOs in `intentforge-api`, JDK `HttpServer` + SSE transport in `boot-server`, virtual-thread request executor wiring, and a terminal-startable demo main; targeted reactor tests now pass for the API and server modules |
| 2026-03-12 22:06:22 +0800 | finished | 100% | updated architecture and module docs, verified a real terminal smoke flow through `AiAssetServerMain` using `curl` for create, SSE replay/live events, resume, and completion, and passed the full `make test` reactor |
| 2026-03-12 22:06:55 +0800 | finished | 100% | task bookkeeping synchronized after docs checkpoint `fd7e884`; the minimal API + boot-server chain is fully closed with virtual-thread-backed request handling |

## Sequence Diagram

```mermaid
sequenceDiagram
    actor User as Terminal User
    participant Main as AiAssetServerMain
    participant Server as AiAssetServerBootstrap
    participant Local as AiAssetLocalBootstrap
    participant Api as AgentRunHttpApi
    participant Broker as AgentRunEventBroker
    participant Gateway as DefaultAgentRunGateway
    participant Router as StageRoutingAgentRouter
    participant Planner as NativePlannerAgent
    participant Coder as NativeCoderAgent
    participant Reviewer as NativeReviewerAgent
    participant Tool as ToolGateway

    User->>Main: start demo server
    Main->>Server: bootstrap(host, port, plugins, seeders)
    Server->>Local: bootstrap local runtime
    Server->>Api: wire HTTP + SSE handlers

    User->>Api: POST /api/agent-runs
    Api->>Gateway: start(task, Broker::publish)
    Gateway->>Router: route(task, context, agents)
    Gateway->>Planner: execute(context, state0)
    Planner-->>Gateway: plan + decision
    Gateway-->>Broker: RUN_CREATED..AWAITING_USER
    Api-->>User: 201 AgentRunResponse

    User->>Api: GET /api/agent-runs/{runId}/events
    Api-->>User: replay historical SSE events

    User->>Api: POST /api/agent-runs/{runId}/messages
    Api->>Gateway: resume(runId, feedback, Broker::publish)
    Gateway->>Coder: execute(context, state1)
    Coder->>Tool: fs/env tool calls
    Tool-->>Coder: tool results
    Coder-->>Gateway: artifact + decision
    Gateway-->>Broker: USER_FEEDBACK_RECEIVED..AWAITING_USER
    Broker-->>User: live SSE events
    Api-->>User: 200 AgentRunResponse

    User->>Api: POST /api/agent-runs/{runId}/messages
    Api->>Gateway: resume(runId, feedback, Broker::publish)
    Gateway->>Reviewer: execute(context, state2)
    Reviewer-->>Gateway: review artifact + decision
    Gateway-->>Broker: USER_FEEDBACK_RECEIVED..RUN_COMPLETED
    Broker-->>User: live SSE events
    Api-->>User: 200 AgentRunResponse
```

## Module Relationship Diagram

```mermaid
flowchart LR
    Client[Terminal Client or curl] --> Api[intentforge-boot-server]
    Api --> Dto[intentforge-api]
    Api --> Local[intentforge-boot-local]
    Api --> Gov[intentforge-governance]
    Gov --> Core[intentforge-agent-core]
    Gov --> Native[intentforge-agent-native]
    Native --> Tool[intentforge-tool]
    Gov --> Session[intentforge-session]
    Gov --> Space[intentforge-space]
    Gov --> PromptModel[prompt model provider]
```
