package cn.intentforge.channel.wecom.shared;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared JSON helpers for the WeCom intelligent-robot connector.
 *
 * @since 1.0.0
 */
public final class WeComJsonSupport {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private WeComJsonSupport() {
  }

  /**
   * Returns the shared JSON mapper.
   *
   * @return shared JSON mapper
   */
  public static ObjectMapper objectMapper() {
    return OBJECT_MAPPER;
  }

  /**
   * Parses one JSON payload into a tree.
   *
   * @param payload raw JSON payload
   * @param errorMessage message used when parsing fails
   * @return parsed JSON tree
   */
  public static JsonNode readTree(String payload, String errorMessage) {
    try {
      return OBJECT_MAPPER.readTree(requireText(payload, "payload"));
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException(errorMessage, exception);
    }
  }

  /**
   * Serializes one value into JSON.
   *
   * @param value value to serialize
   * @param errorMessage message used when serialization fails
   * @return JSON text
   */
  public static String writeValue(Object value, String errorMessage) {
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException(errorMessage, exception);
    }
  }
}
