# intentforge-api

External API contracts, DTOs, and event models.

Current MVP scope:
- `POST /api/agent-runs`
- `GET /api/agent-runs/{runId}/events`
- `POST /api/agent-runs/{runId}/messages`
- `POST /api/agent-runs/{runId}/cancel`

Create-run contract notes:
- `sessionId` is optional on `POST /api/agent-runs`
- when `sessionId` is absent, the server auto-creates one session
- when `sessionId` is absent, `spaceId` must be provided
- the response always returns the effective `sessionId` and `runId`

Source of truth:
- `/Users/clouds3n/Coding/open-source/ai/intent-forge/docs/api-spec.yaml`
