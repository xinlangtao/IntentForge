package cn.intentforge.tool.core.permission;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.tool.core.model.ToolCallRequest;
import cn.intentforge.tool.core.model.ToolExecutionContext;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default permission policy.
 *
 * <p>Sensitive tools use {@link ToolPermissionDecision#ASK} by default. Non-sensitive tools use
 * {@link ToolPermissionDecision#ALLOW} by default. Explicit per-tool overrides always win.</p>
 */
public final class DefaultToolPermissionPolicy implements ToolPermissionPolicy {
  private final Set<String> sensitiveToolIds;
  private final Map<String, ToolPermissionDecision> overrides;

  private volatile ToolPermissionDecision sensitiveDefaultDecision;
  private volatile ToolPermissionDecision nonSensitiveDefaultDecision;

  /**
   * Creates policy with predefined sensitive tool identifiers.
   *
   * @param sensitiveToolIds sensitive tool set
   */
  public DefaultToolPermissionPolicy(Collection<String> sensitiveToolIds) {
    this.sensitiveToolIds = ConcurrentHashMap.newKeySet();
    if (sensitiveToolIds != null) {
      this.sensitiveToolIds.addAll(normalizeAll(sensitiveToolIds));
    }
    this.overrides = new ConcurrentHashMap<>();
    this.sensitiveDefaultDecision = ToolPermissionDecision.ASK;
    this.nonSensitiveDefaultDecision = ToolPermissionDecision.ALLOW;
  }

  /**
   * Creates policy with no initial sensitive tool identifiers.
   */
  public DefaultToolPermissionPolicy() {
    this(Set.of());
  }

  /**
   * Sets the default decision for sensitive tools.
   *
   * @param decision default decision
   */
  public void setSensitiveDefaultDecision(ToolPermissionDecision decision) {
    this.sensitiveDefaultDecision = Objects.requireNonNull(decision, "decision must not be null");
  }

  /**
   * Sets the default decision for non-sensitive tools.
   *
   * @param decision default decision
   */
  public void setNonSensitiveDefaultDecision(ToolPermissionDecision decision) {
    this.nonSensitiveDefaultDecision = Objects.requireNonNull(decision, "decision must not be null");
  }

  /**
   * Marks tools as sensitive.
   *
   * @param toolIds tool identifiers
   */
  public void registerSensitiveTools(Collection<String> toolIds) {
    if (toolIds == null || toolIds.isEmpty()) {
      return;
    }
    sensitiveToolIds.addAll(normalizeAll(toolIds));
  }

  /**
   * Returns current sensitive tools.
   *
   * @return immutable sensitive tool set
   */
  public Set<String> sensitiveToolIds() {
    return Set.copyOf(new LinkedHashSet<>(sensitiveToolIds));
  }

  /**
   * Sets explicit decision for one tool.
   *
   * @param toolId tool identifier
   * @param decision decision to use
   */
  public void setDecision(String toolId, ToolPermissionDecision decision) {
    String normalizedToolId = normalize(toolId);
    if (normalizedToolId == null) {
      throw new IllegalArgumentException("toolId must not be blank");
    }
    overrides.put(normalizedToolId, Objects.requireNonNull(decision, "decision must not be null"));
  }

  /**
   * Sets explicit allow for one tool.
   *
   * @param toolId tool identifier
   */
  public void allow(String toolId) {
    setDecision(toolId, ToolPermissionDecision.ALLOW);
  }

  /**
   * Sets explicit deny for one tool.
   *
   * @param toolId tool identifier
   */
  public void deny(String toolId) {
    setDecision(toolId, ToolPermissionDecision.DENY);
  }

  /**
   * Sets explicit ask for one tool.
   *
   * @param toolId tool identifier
   */
  public void ask(String toolId) {
    setDecision(toolId, ToolPermissionDecision.ASK);
  }

  /**
   * Clears explicit decision for one tool.
   *
   * @param toolId tool identifier
   */
  public void clearDecision(String toolId) {
    String normalizedToolId = normalize(toolId);
    if (normalizedToolId == null) {
      return;
    }
    overrides.remove(normalizedToolId);
  }

  @Override
  public ToolPermissionDecision decide(String toolId, ToolCallRequest request, ToolExecutionContext context) {
    String normalizedToolId = normalize(toolId);
    if (normalizedToolId == null) {
      return ToolPermissionDecision.DENY;
    }
    ToolPermissionDecision override = overrides.get(normalizedToolId);
    if (override != null) {
      return override;
    }
    if (sensitiveToolIds.contains(normalizedToolId)) {
      return sensitiveDefaultDecision;
    }
    return nonSensitiveDefaultDecision;
  }

  private static Set<String> normalizeAll(Collection<String> values) {
    Set<String> normalized = new LinkedHashSet<>();
    for (String value : values) {
      String item = normalize(value);
      if (item != null) {
        normalized.add(item);
      }
    }
    return normalized;
  }
}
