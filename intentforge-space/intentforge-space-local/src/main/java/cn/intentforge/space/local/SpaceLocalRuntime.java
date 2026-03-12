package cn.intentforge.space.local;

import cn.intentforge.space.SpaceRegistry;
import cn.intentforge.space.SpaceResolver;
import java.util.Objects;

/**
 * Groups the local space runtime components used by local bootstraps.
 *
 * @param spaceRegistry mutable registry containing local space definitions
 * @param spaceResolver resolver backed by the local registry
 */
public record SpaceLocalRuntime(
    SpaceRegistry spaceRegistry,
    SpaceResolver spaceResolver
) {
  /**
   * Creates a validated local space runtime container.
   *
   * @param spaceRegistry mutable registry containing local space definitions
   * @param spaceResolver resolver backed by the local registry
   */
  public SpaceLocalRuntime {
    spaceRegistry = Objects.requireNonNull(spaceRegistry, "spaceRegistry must not be null");
    spaceResolver = Objects.requireNonNull(spaceResolver, "spaceResolver must not be null");
  }
}
