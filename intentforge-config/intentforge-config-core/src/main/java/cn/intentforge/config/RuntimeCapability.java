package cn.intentforge.config;

/**
 * Enumerates runtime capabilities whose implementation can be selected by configuration.
 */
public enum RuntimeCapability {
  /**
   * Prompt manager implementation.
   */
  PROMPT_MANAGER,
  /**
   * Model manager implementation.
   */
  MODEL_MANAGER,
  /**
   * Model provider registry implementation.
   */
  MODEL_PROVIDER_REGISTRY,
  /**
   * Tool registry implementation.
   */
  TOOL_REGISTRY,
  /**
   * Session manager implementation.
   */
  SESSION_MANAGER,
  /**
   * Memory store implementation.
   */
  MEMORY_STORE,
  /**
   * Configuration provider or resolver implementation.
   */
  CONFIG_PROVIDER
}
