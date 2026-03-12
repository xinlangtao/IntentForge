package cn.intentforge.boot.local;

import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.agent.core.TaskMode;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AiAssetLocalBootstrapAgentIntegrationTest {
  @Test
  void shouldBootstrapAndExecuteNativeCodingAgentFlow() throws Exception {
    Path workspace = Files.createTempDirectory("boot-agent-workspace");
    Files.writeString(workspace.resolve("README.md"), "agent");
    Path pluginsDirectory = Files.createTempDirectory("boot-agent-plugins");
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

    var result = runtime.agentGateway().execute(new AgentTask(
        "task-1",
        "session-1",
        null,
        workspace,
        TaskMode.FULL,
        "Implement native Java coding agent MVP",
        null,
        Map.of("story", "IF-101")));

    Assertions.assertEquals(3, runtime.agentGateway().listAgents().size());
    Assertions.assertEquals(List.of("prompt-1"), result.contextPack().prompts().stream().map(PromptDefinition::id).toList());
    Assertions.assertEquals(List.of("model-1"), result.contextPack().models().stream().map(ModelDescriptor::id).toList());
    Assertions.assertEquals(List.of("provider-1"),
        result.contextPack().modelProviders().stream().map(ModelProviderDescriptor::id).toList());
    Assertions.assertEquals(3, result.route().steps().size());
    Assertions.assertNotNull(result.plan());
    Assertions.assertEquals(3, result.decisions().size());
    Assertions.assertFalse(result.toolCalls().isEmpty());
  }
}
