package cn.intentforge.boot.local;

import cn.intentforge.agent.core.AgentRunEventType;
import cn.intentforge.agent.core.AgentRunSnapshot;
import cn.intentforge.agent.core.AgentRunStatus;
import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.agent.core.AgentRunTransition;
import cn.intentforge.agent.core.TaskMode;
import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.model.catalog.ModelCapability;
import cn.intentforge.model.catalog.ModelDescriptor;
import cn.intentforge.model.catalog.ModelType;
import cn.intentforge.model.provider.ModelProvider;
import cn.intentforge.model.provider.ModelProviderDescriptor;
import cn.intentforge.model.provider.ModelProviderType;
import cn.intentforge.prompt.model.PromptDefinition;
import cn.intentforge.prompt.model.PromptKind;
import cn.intentforge.session.model.SessionDraft;
import cn.intentforge.space.SpaceDefinition;
import cn.intentforge.space.SpaceProfile;
import cn.intentforge.space.SpaceType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AiAssetLocalBootstrapAgentRunIntegrationTest {
  @Test
  void shouldBootstrapAndRunEventDrivenNativeAgentLifecycle() throws Exception {
    Path workspace = Files.createTempDirectory("boot-agent-run-workspace");
    Files.writeString(workspace.resolve("README.md"), "agent");
    Path pluginsDirectory = Files.createTempDirectory("boot-agent-run-plugins");
    AiAssetLocalRuntime runtime = AiAssetLocalBootstrap.bootstrap(pluginsDirectory, registry -> registry.registerAll(List.of(
        new SpaceDefinition("company-root", SpaceType.COMPANY, null, SpaceProfile.empty()),
        new SpaceDefinition("project-alpha", SpaceType.PROJECT, "company-root", SpaceProfile.empty()),
        new SpaceDefinition("product-alpha", SpaceType.PRODUCT, "project-alpha", SpaceProfile.empty()),
        new SpaceDefinition("application-alpha", SpaceType.APPLICATION, "product-alpha", new SpaceProfile(
            List.of(),
            List.of("intentforge.native.planner", "intentforge.native.coder", "intentforge.native.reviewer"),
            List.of("prompt-1"),
            List.of("intentforge.fs.list", "intentforge.runtime.environment.read"),
            List.of("model-1"),
            List.of("provider-1"),
            List.of(),
            Map.of("review.level", "mvp"))))));

    runtime.promptManager().register(new PromptDefinition(
        "prompt-1",
        "v1",
        "Planner Prompt",
        "prompt",
        PromptKind.SYSTEM,
        List.of(),
        List.of("coding"),
        "Plan the task",
        Map.of()));
    runtime.modelManager().register(new ModelDescriptor(
        "model-1",
        "provider-1",
        "Coding Model",
        "model",
        ModelType.CHAT,
        List.of(ModelCapability.CHAT, ModelCapability.REASONING),
        32000,
        true,
        Map.of()));
    runtime.providerRegistry().register(new ModelProvider() {
      @Override
      public ModelProviderDescriptor descriptor() {
        return new ModelProviderDescriptor(
            "provider-1",
            "Provider",
            "provider",
            ModelProviderType.CUSTOM,
            "native://provider",
            List.of(ModelCapability.CHAT),
            Map.of());
      }
    });
    runtime.sessionManager().create(new SessionDraft("session-1", "Coding", "application-alpha", Map.of()));

    List<String> observedEventTypes = new ArrayList<>();
    AgentRunSnapshot pausedAfterPlanner = runtime.agentRunGateway().start(new AgentTask(
        "task-1",
        "session-1",
        null,
        workspace,
        TaskMode.FULL,
        "Implement event-driven agent runtime",
        null,
        Map.of("story", "IF-301")), event -> observedEventTypes.add(event.type().name()));

    Assertions.assertEquals(AgentRunStatus.AWAITING_USER, pausedAfterPlanner.status());
    Assertions.assertEquals(1, pausedAfterPlanner.nextStepIndex());
    Assertions.assertEquals(1, pausedAfterPlanner.route().steps().size());
    Assertions.assertTrue(pausedAfterPlanner.availableNextActions().stream().anyMatch(action -> action.complete()));
    Assertions.assertTrue(observedEventTypes.contains(AgentRunEventType.AWAITING_USER.name()));
    Assertions.assertEquals(
        "intentforge.prompt.manager.in-memory",
        pausedAfterPlanner.contextPack().runtimeSelection().get(RuntimeCapability.PROMPT_MANAGER).orElseThrow().id());

    AgentRunSnapshot pausedAfterCoder = runtime.agentRunGateway().resume(
        pausedAfterPlanner.runId(),
        new AgentRunTransition("Please review before coding", null, cn.intentforge.agent.core.AgentRole.REVIEWER, false),
        event -> observedEventTypes.add(event.type().name()));
    Assertions.assertEquals(AgentRunStatus.AWAITING_USER, pausedAfterCoder.status());
    Assertions.assertEquals(
        List.of(cn.intentforge.agent.core.AgentRole.PLANNER, cn.intentforge.agent.core.AgentRole.REVIEWER),
        pausedAfterCoder.route().steps().stream().map(step -> step.role()).toList());

    AgentRunSnapshot completed = runtime.agentRunGateway().resume(
        pausedAfterPlanner.runId(),
        new AgentRunTransition("The plan is approved", null, null, true),
        event -> observedEventTypes.add(event.type().name()));

    Assertions.assertEquals(AgentRunStatus.COMPLETED, completed.status());
    Assertions.assertEquals(2, completed.state().decisions().size());
    Assertions.assertTrue(observedEventTypes.contains(AgentRunEventType.RUN_COMPLETED.name()));
    Assertions.assertEquals(
        Map.of(
            "PROMPT_MANAGER", "intentforge.prompt.manager.in-memory",
            "MODEL_MANAGER", "intentforge.model.manager.in-memory",
            "MODEL_PROVIDER_REGISTRY", "intentforge.model-provider.registry.in-memory",
            "TOOL_REGISTRY", "intentforge.tool.registry.in-memory",
            "SESSION_MANAGER", "intentforge.session.manager.in-memory"),
        pausedAfterPlanner.events().stream()
            .filter(event -> event.type() == AgentRunEventType.CONTEXT_RESOLVED)
            .findFirst()
            .orElseThrow()
            .metadata()
            .get("selectedRuntimeIds"));
  }
}
