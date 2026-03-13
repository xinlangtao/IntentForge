package cn.intentforge.prompt.model;

import static cn.intentforge.common.util.ValidationSupport.normalize;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record PromptDefinition(
    String id,
    String version,
    String title,
    String description,
    PromptKind kind,
    List<PromptVariable> variables,
    List<String> tags,
    String template,
    Map<String, String> metadata
) {
  public PromptDefinition {
    id = requireText(id, "id");
    version = normalizeVersion(version);
    title = normalize(title);
    description = normalize(description);
    kind = Objects.requireNonNullElse(kind, PromptKind.USER);
    variables = immutableVariables(variables);
    tags = immutableTags(tags);
    template = requireText(template, "template");
    metadata = immutableMetadata(metadata);
  }

  public boolean matches(PromptQuery query) {
    if (query == null) {
      return true;
    }
    if (query.id() != null && !id.equals(query.id())) {
      return false;
    }
    if (query.version() != null && !version.equals(query.version())) {
      return false;
    }
    if (query.kind() != null && kind != query.kind()) {
      return false;
    }
    return query.tag() == null || tags.contains(query.tag());
  }

  private static String normalizeVersion(String value) {
    String normalized = normalize(value);
    return normalized == null ? "latest" : normalized;
  }

  private static List<PromptVariable> immutableVariables(List<PromptVariable> variables) {
    return variables == null ? List.of() : List.copyOf(variables);
  }

  private static List<String> immutableTags(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return List.of();
    }
    List<String> normalizedTags = new ArrayList<>();
    for (String tag : tags) {
      String normalized = normalize(tag);
      if (normalized == null) {
        continue;
      }
      String lowercase = normalized.toLowerCase(Locale.ROOT);
      if (!normalizedTags.contains(lowercase)) {
        normalizedTags.add(lowercase);
      }
    }
    return List.copyOf(normalizedTags);
  }

  private static Map<String, String> immutableMetadata(Map<String, String> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return Map.of();
    }
    Map<String, String> normalizedMetadata = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : metadata.entrySet()) {
      String key = requireText(entry.getKey(), "metadata key");
      String value = Objects.requireNonNull(entry.getValue(), "metadata value must not be null");
      normalizedMetadata.put(key, value);
    }
    return Map.copyOf(normalizedMetadata);
  }
}
