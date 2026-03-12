package cn.intentforge.space;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Stores space definitions used for hierarchy resolution.
 */
public interface SpaceRegistry {
  /**
   * Registers or replaces a single space definition by identifier.
   *
   * @param spaceDefinition space definition to register
   */
  void register(SpaceDefinition spaceDefinition);

  /**
   * Registers or replaces multiple space definitions.
   *
   * @param spaceDefinitions space definitions to register
   */
  void registerAll(Collection<SpaceDefinition> spaceDefinitions);

  /**
   * Looks up a space definition by identifier.
   *
   * @param id space identifier
   * @return registered space definition if present
   */
  Optional<SpaceDefinition> find(String id);

  /**
   * Lists all registered space definitions.
   *
   * @return immutable snapshot of registered spaces
   */
  List<SpaceDefinition> list();
}
