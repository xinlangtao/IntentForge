package cn.intentforge.boot.local;

import cn.intentforge.config.RuntimeCapability;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AiAssetLocalBootstrapRuntimeCatalogTest {
  @Test
  void shouldExposeBuiltinRuntimeCatalog() throws Exception {
    AiAssetLocalRuntime runtime = AiAssetLocalBootstrap.bootstrap(Files.createTempDirectory("runtime-catalog-plugins"));

    Assertions.assertEquals(
        List.of("intentforge.prompt.manager.in-memory"),
        runtime.runtimeCatalog().list(RuntimeCapability.PROMPT_MANAGER).stream().map(descriptor -> descriptor.id()).toList());
    Assertions.assertEquals(
        List.of("intentforge.model.manager.in-memory"),
        runtime.runtimeCatalog().list(RuntimeCapability.MODEL_MANAGER).stream().map(descriptor -> descriptor.id()).toList());
    Assertions.assertEquals(
        List.of("intentforge.model-provider.registry.in-memory"),
        runtime.runtimeCatalog().list(RuntimeCapability.MODEL_PROVIDER_REGISTRY).stream().map(descriptor -> descriptor.id()).toList());
    Assertions.assertEquals(
        List.of("intentforge.tool.registry.in-memory"),
        runtime.runtimeCatalog().list(RuntimeCapability.TOOL_REGISTRY).stream().map(descriptor -> descriptor.id()).toList());
    Assertions.assertEquals(
        List.of("intentforge.session.manager.in-memory"),
        runtime.runtimeCatalog().list(RuntimeCapability.SESSION_MANAGER).stream().map(descriptor -> descriptor.id()).toList());
  }
}
