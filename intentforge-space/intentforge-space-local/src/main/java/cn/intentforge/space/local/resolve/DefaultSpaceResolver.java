package cn.intentforge.space.local.resolve;

import cn.intentforge.space.ResolvedSpaceProfile;
import cn.intentforge.space.SpaceDefinition;
import cn.intentforge.space.SpaceProfile;
import cn.intentforge.space.SpaceResolutionException;
import cn.intentforge.space.SpaceResolver;
import cn.intentforge.space.SpaceRegistry;
import cn.intentforge.space.SpaceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Default {@link SpaceResolver} that resolves space profiles through parent inheritance.
 */
public final class DefaultSpaceResolver implements SpaceResolver {
  private final SpaceRegistry spaceRegistry;

  /**
   * Creates a resolver backed by the provided registry.
   *
   * @param spaceRegistry space registry that stores hierarchy definitions
   */
  public DefaultSpaceResolver(SpaceRegistry spaceRegistry) {
    this.spaceRegistry = Objects.requireNonNull(spaceRegistry, "spaceRegistry must not be null");
  }

  /**
   * Resolves the effective profile for the provided space identifier.
   *
   * @param spaceId target space identifier
   * @return resolved effective profile
   * @throws SpaceResolutionException when the hierarchy is invalid or incomplete
   */
  @Override
  public ResolvedSpaceProfile resolve(String spaceId) {
    SpaceDefinition target = spaceRegistry.find(requireText(spaceId, "spaceId"))
        .orElseThrow(() -> new SpaceResolutionException("space not found: " + spaceId));
    List<SpaceDefinition> chain = buildChain(target);
    return merge(chain, target);
  }

  private List<SpaceDefinition> buildChain(SpaceDefinition target) {
    List<SpaceDefinition> reversedChain = new ArrayList<>();
    Set<String> visited = new LinkedHashSet<>();
    SpaceDefinition current = target;
    while (current != null) {
      if (!visited.add(current.id())) {
        throw new SpaceResolutionException("cycle detected in space hierarchy: " + current.id());
      }
      reversedChain.add(current);
      if (current.parentId() == null) {
        break;
      }
      SpaceDefinition parent = spaceRegistry.find(current.parentId()).orElse(null);
      if (parent == null) {
        throw new SpaceResolutionException("missing parent for space " + current.id() + ": " + current.parentId());
      }
      if (visited.contains(parent.id())) {
        throw new SpaceResolutionException("cycle detected in space hierarchy: " + parent.id());
      }
      current = parent;
    }
    List<SpaceDefinition> chain = new ArrayList<>(reversedChain.size());
    for (int index = reversedChain.size() - 1; index >= 0; index--) {
      chain.add(reversedChain.get(index));
    }
    validateChain(chain);
    return List.copyOf(chain);
  }

  private void validateChain(List<SpaceDefinition> chain) {
    for (int index = chain.size() - 1; index > 0; index--) {
      SpaceDefinition child = chain.get(index);
      SpaceDefinition parent = chain.get(index - 1);
      validateParentRelation(child, parent);
    }
  }

  private void validateParentRelation(SpaceDefinition child, SpaceDefinition parent) {
    SpaceType expectedParentType = child.type().expectedParentType()
        .orElseThrow(() -> new SpaceResolutionException("company space must not define a parent: " + child.id()));
    if (parent.type() != expectedParentType) {
      throw new SpaceResolutionException(
          "invalid parent type for " + child.id() + ": expected parent type " + expectedParentType
              + " but found " + parent.type());
    }
  }

  private ResolvedSpaceProfile merge(List<SpaceDefinition> chain, SpaceDefinition target) {
    List<String> skillIds = List.of();
    List<String> agentIds = List.of();
    List<String> promptIds = List.of();
    List<String> toolIds = List.of();
    List<String> modelIds = List.of();
    List<String> modelProviderIds = List.of();
    List<String> memoryIds = List.of();
    Map<String, String> config = new LinkedHashMap<>();
    List<String> inheritancePath = new ArrayList<>(chain.size());
    for (SpaceDefinition definition : chain) {
      inheritancePath.add(definition.id());
      SpaceProfile profile = definition.profile();
      if (profile.skillIds() != null) {
        skillIds = profile.skillIds();
      }
      if (profile.agentIds() != null) {
        agentIds = profile.agentIds();
      }
      if (profile.promptIds() != null) {
        promptIds = profile.promptIds();
      }
      if (profile.toolIds() != null) {
        toolIds = profile.toolIds();
      }
      if (profile.modelIds() != null) {
        modelIds = profile.modelIds();
      }
      if (profile.modelProviderIds() != null) {
        modelProviderIds = profile.modelProviderIds();
      }
      if (profile.memoryIds() != null) {
        memoryIds = profile.memoryIds();
      }
      if (profile.config() != null) {
        config.putAll(profile.config());
      }
    }
    return new ResolvedSpaceProfile(
        target.id(),
        target.type(),
        inheritancePath,
        skillIds,
        agentIds,
        promptIds,
        toolIds,
        modelIds,
        modelProviderIds,
        memoryIds,
        config);
  }

  private static String requireText(String value, String fieldName) {
    String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
