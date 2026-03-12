package cn.intentforge.space.local.registry;

import cn.intentforge.space.SpaceDefinition;
import cn.intentforge.space.SpaceRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory {@link SpaceRegistry} implementation for local runtime composition.
 */
public final class InMemorySpaceRegistry implements SpaceRegistry {
  private final Map<String, SpaceDefinition> spacesById = new LinkedHashMap<>();

  /**
   * Registers or replaces a single space definition by identifier.
   *
   * @param spaceDefinition space definition to register
   */
  @Override
  public synchronized void register(SpaceDefinition spaceDefinition) {
    SpaceDefinition definition = Objects.requireNonNull(spaceDefinition, "spaceDefinition must not be null");
    spacesById.put(definition.id(), definition);
  }

  /**
   * Registers or replaces multiple space definitions.
   *
   * @param spaceDefinitions space definitions to register
   */
  @Override
  public synchronized void registerAll(Collection<SpaceDefinition> spaceDefinitions) {
    if (spaceDefinitions == null || spaceDefinitions.isEmpty()) {
      return;
    }
    for (SpaceDefinition definition : spaceDefinitions) {
      register(definition);
    }
  }

  /**
   * Looks up a space definition by identifier.
   *
   * @param id space identifier
   * @return registered space definition if present
   */
  @Override
  public synchronized Optional<SpaceDefinition> find(String id) {
    if (id == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(spacesById.get(id.trim()));
  }

  /**
   * Lists all registered space definitions.
   *
   * @return immutable snapshot of registered spaces
   */
  @Override
  public synchronized List<SpaceDefinition> list() {
    return List.copyOf(new ArrayList<>(spacesById.values()));
  }
}
