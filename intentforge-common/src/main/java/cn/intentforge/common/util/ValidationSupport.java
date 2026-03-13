package cn.intentforge.common.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared validation and normalization helpers for text and immutable collections.
 */
public final class ValidationSupport {
  private ValidationSupport() {
  }

  /**
   * Normalizes one potentially blank text value to trimmed text or {@code null}.
   *
   * @param value raw input
   * @return trimmed text or {@code null}
   */
  public static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  /**
   * Normalizes one optional text value.
   *
   * @param value raw input
   * @return trimmed text or {@code null}
   */
  public static String normalizeOptional(String value) {
    return normalize(value);
  }

  /**
   * Returns one normalized non-blank text value.
   *
   * @param value raw input
   * @param fieldName field name used in validation errors
   * @return normalized text
   */
  public static String requireText(String value, String fieldName) {
    String normalized = normalize(value);
    if (normalized == null) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return normalized;
  }

  /**
   * Returns normalized text when present, otherwise one normalized default value.
   *
   * @param value raw input
   * @param defaultValue fallback value
   * @return normalized input or fallback
   */
  public static String textOrDefault(String value, String defaultValue) {
    String normalized = normalize(value);
    return normalized == null ? requireText(defaultValue, "defaultValue") : normalized;
  }

  /**
   * Returns one immutable normalized string map.
   *
   * @param value raw map
   * @param fieldName field name used in validation errors
   * @return immutable normalized map
   */
  public static Map<String, String> immutableStringMap(Map<String, String> value, String fieldName) {
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

  /**
   * Returns one immutable normalized object map.
   *
   * @param value raw map
   * @param fieldName field name used in validation errors
   * @return immutable normalized map
   */
  public static Map<String, Object> immutableObjectMap(Map<String, Object> value, String fieldName) {
    if (value == null || value.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> normalized = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : value.entrySet()) {
      Objects.requireNonNull(entry, fieldName + " entry must not be null");
      normalized.put(
          requireText(entry.getKey(), fieldName + " key"),
          Objects.requireNonNull(entry.getValue(), fieldName + " value must not be null"));
    }
    return Map.copyOf(normalized);
  }

  /**
   * Returns one immutable list after checking that all items are non-null.
   *
   * @param value raw list
   * @param fieldName field name used in validation errors
   * @param <T> list item type
   * @return immutable validated list
   */
  public static <T> List<T> immutableList(List<T> value, String fieldName) {
    if (value == null || value.isEmpty()) {
      return List.of();
    }
    for (T item : value) {
      Objects.requireNonNull(item, fieldName + " item must not be null");
    }
    return List.copyOf(value);
  }
}
