package cn.intentforge.session.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class SessionModelSupport {
  private SessionModelSupport() {
  }

  static String requireText(String value, String fieldName) {
    String normalized = normalize(value);
    if (normalized == null) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }

  static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  static String normalizeKeyword(String value) {
    String normalized = normalize(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  static List<SessionMessage> immutableMessages(List<SessionMessage> messages) {
    if (messages == null || messages.isEmpty()) {
      return List.of();
    }
    List<SessionMessage> normalized = new ArrayList<>();
    for (SessionMessage message : messages) {
      normalized.add(Objects.requireNonNull(message, "message must not be null"));
    }
    return List.copyOf(normalized);
  }

  static Map<String, String> immutableMetadata(Map<String, String> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return Map.of();
    }
    Map<String, String> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : metadata.entrySet()) {
      Objects.requireNonNull(entry, "metadata entry must not be null");
      normalized.put(
          requireText(entry.getKey(), "metadata key"),
          requireText(entry.getValue(), "metadata value"));
    }
    return Map.copyOf(normalized);
  }
}
