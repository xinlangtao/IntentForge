package cn.intentforge.space.local;

import cn.intentforge.space.SpaceRegistry;
import cn.intentforge.space.local.registry.InMemorySpaceRegistry;
import cn.intentforge.space.local.resolve.DefaultSpaceResolver;
import java.util.function.Consumer;

/**
 * Creates local in-memory space runtime components.
 */
public final class SpaceLocalRuntimeFactory {
  private SpaceLocalRuntimeFactory() {
  }

  /**
   * Creates a local runtime backed by an in-memory registry and resolver.
   *
   * @param spaceConfigurer callback used to register initial space definitions
   * @return local space runtime container
   */
  public static SpaceLocalRuntime create(Consumer<SpaceRegistry> spaceConfigurer) {
    InMemorySpaceRegistry spaceRegistry = new InMemorySpaceRegistry();
    Consumer<SpaceRegistry> configurer = spaceConfigurer == null ? registry -> {
    } : spaceConfigurer;
    configurer.accept(spaceRegistry);
    return new SpaceLocalRuntime(spaceRegistry, new DefaultSpaceResolver(spaceRegistry));
  }
}
