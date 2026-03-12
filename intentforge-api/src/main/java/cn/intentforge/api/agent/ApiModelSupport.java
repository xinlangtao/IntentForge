package cn.intentforge.api.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ApiModelSupport {
  private ApiModelSupport() {
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

  static Map<String, String> immutableStringMap(Map<String, String> value, String fieldName) {
    if (value == null || value.isEmpty()) {
      return Map.of();
    }
    Map<String, String> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : value.entrySet()) {
      Objects.requireNonNull(entry, fieldName + " entry must not be null");
      normalized.put(
          requireText(entry.getKey(), fieldName + " key"),
          requireText(entry.getValue(), fieldName + " value"));
    }
    return Map.copyOf(normalized);
  }

  static Map<String, Object> immutableObjectMap(Map<String, Object> value, String fieldName) {
    if (value == null || value.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : value.entrySet()) {
      Objects.requireNonNull(entry, fieldName + " entry must not be null");
      normalized.put(requireText(entry.getKey(), fieldName + " key"), Objects.requireNonNull(entry.getValue(),
          fieldName + " value must not be null"));
    }
    return Map.copyOf(normalized);
  }

  static <T> List<T> immutableList(List<T> value, String fieldName) {
    if (value == null) {
      return List.of();
    }
    for (T item : value) {
      Objects.requireNonNull(item, fieldName + " item must not be null");
    }
    return List.copyOf(value);
  }
}
