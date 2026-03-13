package cn.intentforge.common.plugin;

import static cn.intentforge.common.util.ValidationSupport.normalize;
import static cn.intentforge.common.util.ValidationSupport.requireText;
import static cn.intentforge.common.util.ValidationSupport.textOrDefault;

import java.util.Objects;
import java.util.Properties;

public record PluginMetadata(
    String id,
    String name,
    String version,
    String description,
    boolean enabled,
    String apiVersion,
    String intentforgeVersion
) {
  public static final String ID_KEY = "plugin.id";
  public static final String NAME_KEY = "plugin.name";
  public static final String VERSION_KEY = "plugin.version";
  public static final String DESCRIPTION_KEY = "plugin.description";
  public static final String ENABLED_KEY = "plugin.enabled";
  public static final String API_VERSION_KEY = "plugin.apiVersion";
  public static final String INTENTFORGE_VERSION_KEY = "plugin.intentforgeVersion";

  public PluginMetadata {
    id = requireText(id, "plugin.id");
    name = normalize(name);
    version = normalize(version);
    description = normalize(description);
    apiVersion = normalize(apiVersion);
    intentforgeVersion = normalize(intentforgeVersion);
  }

  public static PluginMetadata from(Properties properties, String fallbackId) {
    Objects.requireNonNull(properties, "properties must not be null");
    String fallback = requireText(fallbackId, "fallbackId");
    String id = textOrDefault(properties.getProperty(ID_KEY), fallback);
    String name = textOrDefault(properties.getProperty(NAME_KEY), id);
    String version = textOrDefault(properties.getProperty(VERSION_KEY), "unspecified");
    String description = normalize(properties.getProperty(DESCRIPTION_KEY));
    boolean enabled = !"false".equalsIgnoreCase(normalize(properties.getProperty(ENABLED_KEY)));
    String apiVersion = normalize(properties.getProperty(API_VERSION_KEY));
    String intentforgeVersion = normalize(properties.getProperty(INTENTFORGE_VERSION_KEY));
    return new PluginMetadata(id, name, version, description, enabled, apiVersion, intentforgeVersion);
  }
}
