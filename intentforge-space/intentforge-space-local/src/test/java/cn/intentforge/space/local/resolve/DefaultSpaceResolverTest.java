package cn.intentforge.space.local.resolve;

import cn.intentforge.space.ResolvedSpaceProfile;
import cn.intentforge.space.SpaceDefinition;
import cn.intentforge.space.SpaceProfile;
import cn.intentforge.space.SpaceResolutionException;
import cn.intentforge.space.SpaceType;
import cn.intentforge.space.local.registry.InMemorySpaceRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultSpaceResolverTest {
  @Test
  void shouldResolveProfilesAcrossCompanyProjectProductAndApplication() {
    InMemorySpaceRegistry registry = new InMemorySpaceRegistry();
    registry.registerAll(List.of(
        new SpaceDefinition(
            "application-alpha",
            SpaceType.APPLICATION,
            "product-alpha",
            new SpaceProfile(
                null,
                null,
                null,
                List.of(),
                null,
                null,
                List.of("memory-app", "memory-audit"),
                Map.of("temperature", "0.2"))),
        new SpaceDefinition(
            "company-root",
            SpaceType.COMPANY,
            null,
            new SpaceProfile(
                List.of("skill-company"),
                List.of("agent-company"),
                List.of("prompt-company"),
                List.of("tool-company"),
                null,
                List.of("provider-company"),
                List.of("memory-company"),
                Map.of("timeout", "30", "region", "cn"))),
        new SpaceDefinition(
            "product-alpha",
            SpaceType.PRODUCT,
            "project-alpha",
            new SpaceProfile(
                List.of("skill-product"),
                null,
                null,
                null,
                List.of("model-product", "model-shadow"),
                null,
                null,
                Map.of("temperature", "0.1"))),
        new SpaceDefinition(
            "project-alpha",
            SpaceType.PROJECT,
            "company-root",
            new SpaceProfile(
                null,
                null,
                null,
                List.of("tool-project"),
                null,
                List.of("provider-project"),
                List.of("memory-project"),
                Map.of("region", "apac", "projectKey", "alpha")))));

    ResolvedSpaceProfile resolved = new DefaultSpaceResolver(registry).resolve("application-alpha");

    Assertions.assertEquals("application-alpha", resolved.spaceId());
    Assertions.assertEquals(SpaceType.APPLICATION, resolved.spaceType());
    Assertions.assertEquals(List.of("company-root", "project-alpha", "product-alpha", "application-alpha"),
        resolved.inheritancePath());
    Assertions.assertEquals(List.of("skill-product"), resolved.skillIds());
    Assertions.assertEquals(List.of("agent-company"), resolved.agentIds());
    Assertions.assertEquals(List.of("prompt-company"), resolved.promptIds());
    Assertions.assertEquals(List.of(), resolved.toolIds());
    Assertions.assertEquals(List.of("model-product", "model-shadow"), resolved.modelIds());
    Assertions.assertEquals(List.of("provider-project"), resolved.modelProviderIds());
    Assertions.assertEquals(List.of("memory-app", "memory-audit"), resolved.memoryIds());
    Assertions.assertEquals(
        Map.of("timeout", "30", "region", "apac", "projectKey", "alpha", "temperature", "0.2"),
        resolved.config());
  }

  @Test
  void shouldResolveCompanySpaceWithoutParent() {
    InMemorySpaceRegistry registry = new InMemorySpaceRegistry();
    registry.register(new SpaceDefinition(
        "company-root",
        SpaceType.COMPANY,
        null,
        new SpaceProfile(
            List.of("skill-company"),
            List.of("agent-company"),
            List.of("prompt-company"),
            List.of("tool-company"),
            List.of("model-company"),
            List.of("provider-company"),
            List.of("memory-company"),
            Map.of("region", "cn"))));

    ResolvedSpaceProfile resolved = new DefaultSpaceResolver(registry).resolve("company-root");

    Assertions.assertEquals(List.of("company-root"), resolved.inheritancePath());
    Assertions.assertEquals(List.of("skill-company"), resolved.skillIds());
    Assertions.assertEquals(List.of("agent-company"), resolved.agentIds());
    Assertions.assertEquals(List.of("prompt-company"), resolved.promptIds());
    Assertions.assertEquals(List.of("tool-company"), resolved.toolIds());
    Assertions.assertEquals(List.of("model-company"), resolved.modelIds());
    Assertions.assertEquals(List.of("provider-company"), resolved.modelProviderIds());
    Assertions.assertEquals(List.of("memory-company"), resolved.memoryIds());
    Assertions.assertEquals(Map.of("region", "cn"), resolved.config());
  }

  @Test
  void shouldRejectInvalidParentTypeChain() {
    InMemorySpaceRegistry registry = new InMemorySpaceRegistry();
    registry.registerAll(List.of(
        new SpaceDefinition("company-root", SpaceType.COMPANY, null, SpaceProfile.empty()),
        new SpaceDefinition("product-alpha", SpaceType.PRODUCT, "company-root", SpaceProfile.empty())));

    SpaceResolutionException exception = Assertions.assertThrows(
        SpaceResolutionException.class,
        () -> new DefaultSpaceResolver(registry).resolve("product-alpha"));

    Assertions.assertTrue(exception.getMessage().contains("expected parent type PROJECT"));
  }

  @Test
  void shouldRejectCycleAndMissingParent() {
    InMemorySpaceRegistry cycleRegistry = new InMemorySpaceRegistry();
    cycleRegistry.registerAll(List.of(
        new SpaceDefinition("company-root", SpaceType.COMPANY, null, SpaceProfile.empty()),
        new SpaceDefinition("project-alpha", SpaceType.PROJECT, "product-alpha", SpaceProfile.empty()),
        new SpaceDefinition("product-alpha", SpaceType.PRODUCT, "project-alpha", SpaceProfile.empty())));

    SpaceResolutionException cycleException = Assertions.assertThrows(
        SpaceResolutionException.class,
        () -> new DefaultSpaceResolver(cycleRegistry).resolve("project-alpha"));
    Assertions.assertTrue(cycleException.getMessage().contains("cycle"));

    InMemorySpaceRegistry missingParentRegistry = new InMemorySpaceRegistry();
    missingParentRegistry.register(new SpaceDefinition(
        "application-alpha",
        SpaceType.APPLICATION,
        "product-alpha",
        SpaceProfile.empty()));

    SpaceResolutionException missingParentException = Assertions.assertThrows(
        SpaceResolutionException.class,
        () -> new DefaultSpaceResolver(missingParentRegistry).resolve("application-alpha"));
    Assertions.assertTrue(missingParentException.getMessage().contains("missing parent"));
  }
}
