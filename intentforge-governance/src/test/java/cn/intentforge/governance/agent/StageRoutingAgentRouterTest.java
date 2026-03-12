package cn.intentforge.governance.agent;

import cn.intentforge.agent.core.AgentDescriptor;
import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentRole;
import cn.intentforge.agent.core.AgentRoute;
import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.agent.core.ContextPack;
import cn.intentforge.agent.core.TaskMode;
import cn.intentforge.session.model.Session;
import cn.intentforge.session.model.SessionStatus;
import cn.intentforge.space.ResolvedSpaceProfile;
import cn.intentforge.space.SpaceType;
import cn.intentforge.tool.core.model.ToolExecutionContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StageRoutingAgentRouterTest {
  @Test
  void shouldBuildFullRouteUsingSpaceBindings() throws Exception {
    StageRoutingAgentRouter router = new StageRoutingAgentRouter();
    ContextPack contextPack = contextPack(
        new AgentTask("task-1", "session-1", "application-alpha", Files.createTempDirectory("router-full"),
            TaskMode.FULL, "Implement routing", null, Map.of()),
        List.of("intentforge.native.planner", "intentforge.native.coder", "intentforge.native.reviewer"));

    AgentRoute route = router.route(contextPack.task(), contextPack, List.of(
        new AgentDescriptor("intentforge.native.planner", AgentRole.PLANNER, "Planner", "planner"),
        new AgentDescriptor("intentforge.native.coder", AgentRole.CODER, "Coder", "coder"),
        new AgentDescriptor("intentforge.native.reviewer", AgentRole.REVIEWER, "Reviewer", "reviewer")));

    Assertions.assertEquals("stage-based", route.strategy());
    Assertions.assertEquals(
        List.of("intentforge.native.planner", "intentforge.native.coder", "intentforge.native.reviewer"),
        route.steps().stream().map(step -> step.agentId()).toList());
  }

  @Test
  void shouldRejectWhenRequiredStageAgentIsMissing() throws Exception {
    StageRoutingAgentRouter router = new StageRoutingAgentRouter();
    ContextPack contextPack = contextPack(
        new AgentTask("task-1", "session-1", "application-alpha", Files.createTempDirectory("router-missing"),
            TaskMode.FULL, "Implement routing", null, Map.of()),
        List.of("intentforge.native.planner", "intentforge.native.coder"));

    AgentExecutionException exception = Assertions.assertThrows(AgentExecutionException.class, () -> router.route(
        contextPack.task(),
        contextPack,
        List.of(new AgentDescriptor("intentforge.native.planner", AgentRole.PLANNER, "Planner", "planner"))));
    Assertions.assertTrue(exception.getMessage().contains("CODER"));
  }

  private static ContextPack contextPack(AgentTask task, List<String> agentIds) {
    Instant now = Instant.parse("2026-03-12T10:00:00Z");
    return new ContextPack(
        task,
        new Session(task.sessionId(), "Session", task.spaceId(), SessionStatus.ACTIVE, List.of(), Map.of(), now, now),
        new ResolvedSpaceProfile(
            task.spaceId(),
            SpaceType.APPLICATION,
            List.of("company-root", "project-alpha", "product-alpha", task.spaceId()),
            List.of(),
            agentIds,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of()),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        ToolExecutionContext.create(task.workspaceRoot()));
  }
}
