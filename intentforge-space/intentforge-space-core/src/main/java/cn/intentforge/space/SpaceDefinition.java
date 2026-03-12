package cn.intentforge.space;

import java.util.Objects;

/**
 * Declares a single named space and its direct parent relation.
 *
 * @param id unique space identifier
 * @param type hierarchical space type
 * @param parentId direct parent space identifier; required for non-company spaces
 * @param profile resources and configuration defined on the space
 */
public record SpaceDefinition(
    String id,
    SpaceType type,
    String parentId,
    SpaceProfile profile
) {
  /**
   * Creates a validated immutable space definition.
   *
   * @param id unique space identifier
   * @param type hierarchical space type
   * @param parentId direct parent space identifier
   * @param profile resources and configuration defined on the space
   */
  public SpaceDefinition {
    id = normalizeId(id, "id");
    type = Objects.requireNonNull(type, "type must not be null");
    parentId = normalizeParentId(parentId, type);
    profile = Objects.requireNonNull(profile, "profile must not be null");
  }

  private static String normalizeParentId(String parentId, SpaceType type) {
    if (parentId == null) {
      if (type == SpaceType.COMPANY) {
        return null;
      }
      throw new IllegalArgumentException("parentId must not be null for " + type);
    }
    String normalized = normalizeId(parentId, "parentId");
    if (type == SpaceType.COMPANY) {
      throw new IllegalArgumentException("company space must not define parentId");
    }
    return normalized;
  }

  private static String normalizeId(String value, String fieldName) {
    String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }
}
