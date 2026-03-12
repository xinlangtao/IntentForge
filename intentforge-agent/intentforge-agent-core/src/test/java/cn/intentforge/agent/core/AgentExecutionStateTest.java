package cn.intentforge.agent.core;

import cn.intentforge.tool.core.model.ToolCallResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgentExecutionStateTest {
  @Test
  void shouldAppendRunMessagesWithoutDroppingAccumulatedState() {
    Plan plan = new Plan(
        "Plan",
        List.of(new PlanStep("step-1", "Inspect", "Inspect workspace", null, true)),
        Map.of("promptId", "prompt-1"));
    Decision decision = new Decision("agent-1", AgentRole.PLANNER, "planner done", Map.of());
    Artifact artifact = new Artifact("plan.md", "text/markdown", "content", Map.of());
    ToolCallResult toolCall = ToolCallResult.success("ok");
    AgentExecutionState state = AgentExecutionState.empty().merge(
        new AgentStepResult(plan, decision, List.of(artifact), List.of(toolCall)));

    AgentRunMessage message = new AgentRunMessage(
        "message-1",
        AgentRunMessageRole.USER,
        "Refine planner output",
        Instant.parse("2026-03-12T12:00:00Z"),
        Map.of("turn", "1"));
    AgentExecutionState updated = state.appendMessage(message);

    Assertions.assertEquals(plan, updated.plan());
    Assertions.assertEquals(List.of(decision), updated.decisions());
    Assertions.assertEquals(List.of(artifact), updated.artifacts());
    Assertions.assertEquals(List.of(toolCall), updated.toolCalls());
    Assertions.assertEquals(List.of(message), updated.messages());
  }

  @Test
  void shouldRejectBlankRunMessageContent() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new AgentRunMessage(
        "message-1",
        AgentRunMessageRole.USER,
        " ",
        Instant.parse("2026-03-12T12:00:00Z"),
        Map.of()));
  }
}
