package cn.intentforge.boot.local;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import cn.intentforge.channel.registry.ChannelManager;
import cn.intentforge.model.provider.registry.ModelProviderRegistry;
import cn.intentforge.model.registry.ModelManager;
import cn.intentforge.prompt.registry.PromptManager;
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.tool.core.gateway.ToolGateway;
import cn.intentforge.tool.core.registry.ToolRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds all discovered runtime component instances keyed by their stable implementation identifiers.
 *
 * @param channelManagers channel manager instances keyed by implementation identifier
 * @param promptManagers prompt manager instances keyed by implementation identifier
 * @param modelManagers model manager instances keyed by implementation identifier
 * @param modelProviderRegistries model-provider registry instances keyed by implementation identifier
 * @param toolRegistries tool registry instances keyed by implementation identifier
 * @param toolGateways tool gateway instances keyed by implementation identifier
 * @param sessionManagers session manager instances keyed by implementation identifier
 */
public record LocalRuntimeComponentRegistry(
    Map<String, ChannelManager> channelManagers,
    Map<String, PromptManager> promptManagers,
    Map<String, ModelManager> modelManagers,
    Map<String, ModelProviderRegistry> modelProviderRegistries,
    Map<String, ToolRegistry> toolRegistries,
    Map<String, ToolGateway> toolGateways,
    Map<String, SessionManager> sessionManagers
) {
  /**
   * Creates a validated immutable component registry.
   */
  public LocalRuntimeComponentRegistry {
    channelManagers = immutableComponents(channelManagers, "channelManagers");
    promptManagers = immutableComponents(promptManagers, "promptManagers");
    modelManagers = immutableComponents(modelManagers, "modelManagers");
    modelProviderRegistries = immutableComponents(modelProviderRegistries, "modelProviderRegistries");
    toolRegistries = immutableComponents(toolRegistries, "toolRegistries");
    toolGateways = immutableComponents(toolGateways, "toolGateways");
    sessionManagers = immutableComponents(sessionManagers, "sessionManagers");
  }

  /**
   * Returns the channel manager for the provided implementation identifier.
   *
   * @param implementationId runtime implementation identifier
   * @return matching channel manager
   */
  public ChannelManager channelManager(String implementationId) {
    return requireComponent(channelManagers, implementationId, "channel manager");
  }

  /**
   * Returns the prompt manager for the provided implementation identifier.
   *
   * @param implementationId runtime implementation identifier
   * @return matching prompt manager
   */
  public PromptManager promptManager(String implementationId) {
    return requireComponent(promptManagers, implementationId, "prompt manager");
  }

  /**
   * Returns the model manager for the provided implementation identifier.
   *
   * @param implementationId runtime implementation identifier
   * @return matching model manager
   */
  public ModelManager modelManager(String implementationId) {
    return requireComponent(modelManagers, implementationId, "model manager");
  }

  /**
   * Returns the model-provider registry for the provided implementation identifier.
   *
   * @param implementationId runtime implementation identifier
   * @return matching model-provider registry
   */
  public ModelProviderRegistry modelProviderRegistry(String implementationId) {
    return requireComponent(modelProviderRegistries, implementationId, "model-provider registry");
  }

  /**
   * Returns the tool registry for the provided implementation identifier.
   *
   * @param implementationId runtime implementation identifier
   * @return matching tool registry
   */
  public ToolRegistry toolRegistry(String implementationId) {
    return requireComponent(toolRegistries, implementationId, "tool registry");
  }

  /**
   * Returns the tool gateway for the provided implementation identifier.
   *
   * @param implementationId runtime implementation identifier
   * @return matching tool gateway
   */
  public ToolGateway toolGateway(String implementationId) {
    return requireComponent(toolGateways, implementationId, "tool gateway");
  }

  /**
   * Returns the session manager for the provided implementation identifier.
   *
   * @param implementationId runtime implementation identifier
   * @return matching session manager
   */
  public SessionManager sessionManager(String implementationId) {
    return requireComponent(sessionManagers, implementationId, "session manager");
  }

  private static <T> Map<String, T> immutableComponents(Map<String, T> values, String fieldName) {
    Map<String, T> source = Objects.requireNonNull(values, fieldName + " must not be null");
    Map<String, T> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, T> entry : source.entrySet()) {
      Objects.requireNonNull(entry, fieldName + " entry must not be null");
      String implementationId = requireText(entry.getKey(), fieldName + " key");
      T component = Objects.requireNonNull(entry.getValue(), fieldName + " value must not be null");
      normalized.put(implementationId, component);
    }
    return Map.copyOf(normalized);
  }

  private static <T> T requireComponent(Map<String, T> components, String implementationId, String componentName) {
    String normalizedId = requireText(implementationId, componentName + " implementationId");
    T component = components.get(normalizedId);
    if (component == null) {
      throw new IllegalArgumentException(componentName + " not found: " + normalizedId);
    }
    return component;
  }
}
