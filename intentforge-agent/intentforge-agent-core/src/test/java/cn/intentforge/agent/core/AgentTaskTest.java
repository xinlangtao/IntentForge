package cn.intentforge.agent.core;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgentTaskTest {
  @Test
  void shouldNormalizeFieldsAndDefaultMode() {
    AgentTask task = new AgentTask(
        " task-1 ",
        " session-1 ",
        " application-alpha ",
        Path.of("."),
        null,
        " Implement native coding agent ",
        " intentforge.native.coder ",
        Map.of(" lane ", " fast "));

    Assertions.assertEquals("task-1", task.id());
    Assertions.assertEquals("session-1", task.sessionId());
    Assertions.assertEquals("application-alpha", task.spaceId());
    Assertions.assertTrue(task.workspaceRoot().isAbsolute());
    Assertions.assertEquals(TaskMode.FULL, task.mode());
    Assertions.assertEquals("Implement native coding agent", task.intent());
    Assertions.assertEquals("intentforge.native.coder", task.targetAgentId());
    Assertions.assertEquals(Map.of("lane", "fast"), task.metadata());
  }

  @Test
  void shouldRejectBlankRequiredFields() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new AgentTask(
        " ",
        "session-1",
        null,
        Path.of("."),
        TaskMode.PLAN_ONLY,
        "intent",
        null,
        Map.of()));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new AgentTask(
        "task-1",
        " ",
        null,
        Path.of("."),
        TaskMode.PLAN_ONLY,
        "intent",
        null,
        Map.of()));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new AgentTask(
        "task-1",
        "session-1",
        null,
        Path.of("."),
        TaskMode.PLAN_ONLY,
        " ",
        null,
        Map.of()));
  }
}
