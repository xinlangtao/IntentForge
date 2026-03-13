package cn.intentforge.agent.core;

import java.util.Objects;

/**
 * Public metadata describing one available agent executor.
 *
 * @param id stable agent identifier
 * @param role execution role served by the agent
 * @param displayName human readable name
 * @param description short capability summary
 */
public record AgentDescriptor(
    String id,
    AgentRole role,
    String displayName,
    String description
) {
  /**
   * Creates a validated descriptor.
   */
  public AgentDescriptor {
    id = cn.intentforge.common.util.ValidationSupport.requireText(id, "id");
    role = Objects.requireNonNull(role, "role must not be null");
    displayName = cn.intentforge.common.util.ValidationSupport.requireText(displayName, "displayName");
    description = cn.intentforge.common.util.ValidationSupport.requireText(description, "description");
  }
}
