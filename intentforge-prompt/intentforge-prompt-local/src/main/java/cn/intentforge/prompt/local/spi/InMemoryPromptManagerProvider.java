package cn.intentforge.prompt.local.spi;

import cn.intentforge.prompt.local.registry.InMemoryPromptManager;
import cn.intentforge.prompt.registry.PromptManager;
import cn.intentforge.prompt.spi.PromptManagerProvider;

/**
 * Default local {@link PromptManagerProvider} backed by {@link InMemoryPromptManager}.
 */
public final class InMemoryPromptManagerProvider implements PromptManagerProvider {
  /**
   * Uses the base priority so custom providers can override it with a higher value.
   *
   * @return provider priority
   */
  @Override
  public int priority() {
    return 0;
  }

  /**
   * Creates the in-memory prompt manager.
   *
   * @param classLoader class loader used by the runtime
   * @return in-memory prompt manager
   */
  @Override
  public PromptManager create(ClassLoader classLoader) {
    return new InMemoryPromptManager(classLoader);
  }
}
