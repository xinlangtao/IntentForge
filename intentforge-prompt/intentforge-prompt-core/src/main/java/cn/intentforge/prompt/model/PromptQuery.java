package cn.intentforge.prompt.model;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import java.util.Locale;

public record PromptQuery(
    String id,
    String version,
    PromptKind kind,
    String tag
) {
  public PromptQuery {
    id = normalize(id);
    version = normalize(version);
    tag = normalizeTag(tag);
  }

  public static PromptQuery byId(String id) {
    return new PromptQuery(id, null, null, null);
  }

  public static PromptQuery byKind(PromptKind kind) {
    return new PromptQuery(null, null, kind, null);
  }

  public static PromptQuery byTag(String tag) {
    return new PromptQuery(null, null, null, tag);
  }
  private static String normalizeTag(String value) {
    String normalized = normalize(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }
}
