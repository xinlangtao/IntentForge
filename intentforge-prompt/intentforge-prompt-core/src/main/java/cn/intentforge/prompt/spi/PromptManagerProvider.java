package cn.intentforge.prompt.spi;

import cn.intentforge.prompt.registry.PromptManager;

/**
 * Supplies a {@link PromptManager} implementation for runtime composition.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.
 * Higher {@link #priority()} values win when multiple providers are present.
 */
public interface PromptManagerProvider {
  /**
   * Provider priority for conflict resolution.
   *
   * <p>Higher values are preferred.
   *
   * @return provider priority
   */
  default int priority() {
    return 0;
  }

  /**
   * Creates a prompt manager instance.
   *
   * @param classLoader class loader used by the runtime
   * @return prompt manager instance
   */
  PromptManager create(ClassLoader classLoader);
}
