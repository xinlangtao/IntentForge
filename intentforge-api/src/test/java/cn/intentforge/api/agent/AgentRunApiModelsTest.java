package cn.intentforge.api.agent;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgentRunApiModelsTest {
  @Test
  void shouldCreateApiTransportModels() {
    AgentRunCreateRequest createRequest = new AgentRunCreateRequest(
        "task-1",
        null,
        null,
        "/tmp/workspace",
        "FULL",
        "Implement event-driven server",
        null,
        Map.of("story", "IF-401"));
    AgentRunFeedbackRequest feedbackRequest = new AgentRunFeedbackRequest("Please add validation", "CODER", null, false);
    AgentRunCancelRequest cancelRequest = new AgentRunCancelRequest("User stopped the run");
    AgentRunEventResponse eventResponse = new AgentRunEventResponse(
        "agent-run-1",
        1L,
        "RUN_CREATED",
        "RUNNING",
        "run created",
        Map.of("taskId", "task-1"),
        "2026-03-12T13:00:00Z");
    RuntimeImplementationResponse selectedRuntime = new RuntimeImplementationResponse(
        "intentforge.prompt.manager.in-memory",
        "PROMPT_MANAGER",
        "In-Memory Prompt Manager",
        "nightly-SNAPSHOT",
        "cn.intentforge.prompt.local.registry.InMemoryPromptManager",
        Map.of("builtin", "true"));
    AgentRouteStepResponse routeStep = new AgentRouteStepResponse(
        1,
        "intentforge.native.planner",
        "PLANNER",
        "initial planner");
    AgentRunActionResponse actionResponse = new AgentRunActionResponse(
        "intentforge.native.coder",
        "CODER",
        true,
        false,
        "implement the approved plan");
    AgentRunResponse response = new AgentRunResponse(
        "agent-run-1",
        "task-1",
        "session-1",
        "AWAITING_USER",
        1,
        "awaiting user feedback before continuing to CODER",
        "/api/agent-runs/agent-run-1/events",
        List.of(selectedRuntime),
        List.of(routeStep),
        List.of(actionResponse),
        List.of(eventResponse));

    Assertions.assertEquals("FULL", createRequest.mode());
    Assertions.assertNull(createRequest.sessionId());
    Assertions.assertEquals("Please add validation", feedbackRequest.content());
    Assertions.assertEquals("CODER", feedbackRequest.nextRole());
    Assertions.assertEquals("User stopped the run", cancelRequest.reason());
    Assertions.assertEquals("RUN_CREATED", response.events().getFirst().type());
    Assertions.assertEquals("PROMPT_MANAGER", response.selectedRuntimes().getFirst().capability());
    Assertions.assertEquals("nightly-SNAPSHOT", response.selectedRuntimes().getFirst().version());
    Assertions.assertEquals("intentforge.native.planner", response.selectedRouteSteps().getFirst().agentId());
    Assertions.assertEquals("intentforge.native.coder", response.availableNextActions().getFirst().agentId());
    Assertions.assertEquals("/api/agent-runs/agent-run-1/events", response.eventsPath());
  }

  @Test
  void shouldRejectBlankFeedbackAndCancelReason() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new AgentRunFeedbackRequest("   ", null, null, false));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new AgentRunFeedbackRequest("done", "CODER", null, true));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new AgentRunCancelRequest("   "));
  }

  @Test
  void shouldRejectBlankSessionIdWhenProvided() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new AgentRunCreateRequest(
        "task-1",
        "   ",
        "application-alpha",
        "/tmp/workspace",
        "FULL",
        "Implement event-driven server",
        null,
        Map.of()));
  }
}
