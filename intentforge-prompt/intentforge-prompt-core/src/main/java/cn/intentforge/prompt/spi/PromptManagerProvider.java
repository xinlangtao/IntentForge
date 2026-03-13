package cn.intentforge.prompt.spi;

import cn.intentforge.config.RuntimeCapability;
import cn.intentforge.config.RuntimeImplementationDescriptor;
import cn.intentforge.prompt.registry.PromptManager;
import java.util.Map;

/**
 * Supplies a {@link PromptManager} implementation for runtime composition.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader}.
 * Higher {@link #priority()} values win when multiple providers are present.
 */
public interface PromptManagerProvider {
  /**
   * Describes the provider for runtime catalog assembly and configuration binding.
   *
   * @return implementation descriptor
   */
  default RuntimeImplementationDescriptor descriptor() {
    return new RuntimeImplementationDescriptor(
        getClass().getName(),
        RuntimeCapability.PROMPT_MANAGER,
        getClass().getSimpleName(),
        RuntimeImplementationDescriptor.detectVersion(getClass()),
        getClass().getName(),
        Map.of());
  }

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
