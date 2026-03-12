package cn.intentforge.session.local;

import cn.intentforge.session.local.registry.InMemorySessionManager;
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.session.spi.SessionManagerProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Creates local session runtime components from classpath SPI providers.
 */
public final class SessionLocalRuntimeFactory {
  private SessionLocalRuntimeFactory() {
  }

  /**
   * Creates a local runtime using the current thread context class loader.
   *
   * @return local session runtime
   */
  public static SessionLocalRuntime create() {
    return create(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates a local runtime using the provided class loader.
   *
   * @param classLoader class loader used for provider discovery
   * @return local session runtime
   */
  public static SessionLocalRuntime create(ClassLoader classLoader) {
    ClassLoader effectiveClassLoader = classLoader == null
        ? SessionLocalRuntimeFactory.class.getClassLoader()
        : classLoader;
    SessionManager sessionManager = selectSingleProvider(
        effectiveClassLoader,
        SessionManagerProvider.class,
        SessionManagerProvider::priority,
        provider -> provider.create(effectiveClassLoader),
        () -> new InMemorySessionManager(effectiveClassLoader));
    return new SessionLocalRuntime(sessionManager);
  }

  static <P, R> R selectSingleProvider(
      ClassLoader classLoader,
      Class<P> providerType,
      ToIntFunction<P> priorityExtractor,
      Function<P, R> instanceExtractor,
      Supplier<R> defaultSupplier
  ) {
    List<P> providers = ServiceLoader.load(providerType, classLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .toList();
    return selectSingleProvider(providers, providerType, priorityExtractor, instanceExtractor, defaultSupplier);
  }

  static <P, R> R selectSingleProvider(
      List<P> providers,
      Class<P> providerType,
      ToIntFunction<P> priorityExtractor,
      Function<P, R> instanceExtractor,
      Supplier<R> defaultSupplier
  ) {
    if (providers == null || providers.isEmpty()) {
      return defaultSupplier.get();
    }

    P winner = null;
    int winnerPriority = Integer.MIN_VALUE;
    List<P> candidates = new ArrayList<>();
    for (P provider : providers) {
      int priority = priorityExtractor.applyAsInt(provider);
      if (winner == null || priority > winnerPriority) {
        winner = provider;
        winnerPriority = priority;
        candidates.clear();
        candidates.add(provider);
        continue;
      }
      if (priority == winnerPriority) {
        candidates.add(provider);
      }
    }

    if (candidates.size() > 1) {
      String providerNames = candidates.stream()
          .map(provider -> provider.getClass().getName())
          .sorted()
          .reduce((left, right) -> left + ", " + right)
          .orElse("");
      throw new IllegalStateException(
          "multiple " + providerType.getSimpleName() + " with priority " + winnerPriority + ": " + providerNames);
    }

    return Objects.requireNonNull(
        instanceExtractor.apply(winner),
        providerType.getSimpleName() + " returned null runtime component");
  }
}
