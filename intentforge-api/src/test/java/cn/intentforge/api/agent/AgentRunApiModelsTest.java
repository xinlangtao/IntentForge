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
        "session-1",
        null,
        "/tmp/workspace",
        "FULL",
        "Implement event-driven server",
        null,
        Map.of("story", "IF-401"));
    AgentRunFeedbackRequest feedbackRequest = new AgentRunFeedbackRequest("Please add validation");
    AgentRunCancelRequest cancelRequest = new AgentRunCancelRequest("User stopped the run");
    AgentRunEventResponse eventResponse = new AgentRunEventResponse(
        "agent-run-1",
        1L,
        "RUN_CREATED",
        "RUNNING",
        "run created",
        Map.of("taskId", "task-1"),
        "2026-03-12T13:00:00Z");
    AgentRunResponse response = new AgentRunResponse(
        "agent-run-1",
        "task-1",
        "session-1",
        "AWAITING_USER",
        1,
        "awaiting user feedback before continuing to CODER",
        "/api/agent-runs/agent-run-1/events",
        List.of(eventResponse));

    Assertions.assertEquals("FULL", createRequest.mode());
    Assertions.assertEquals("Please add validation", feedbackRequest.content());
    Assertions.assertEquals("User stopped the run", cancelRequest.reason());
    Assertions.assertEquals("RUN_CREATED", response.events().getFirst().type());
    Assertions.assertEquals("/api/agent-runs/agent-run-1/events", response.eventsPath());
  }

  @Test
  void shouldRejectBlankFeedbackAndCancelReason() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new AgentRunFeedbackRequest("   "));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new AgentRunCancelRequest("   "));
  }
}
