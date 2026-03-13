package cn.intentforge.tool.core.model;

import static cn.intentforge.common.util.ValidationSupport.normalize;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public definition of a tool.
 *
 * @param id tool identifier
 * @param description tool description for model/runtime
 * @param parametersSchema JSON-schema-like parameter schema
 * @param sensitive whether this tool is sensitive for permission policy
 */
public record ToolDefinition(
    String id,
    String description,
    Map<String, Object> parametersSchema,
    boolean sensitive
) {
  /**
   * Creates a tool definition.
   *
   * @param id tool identifier
   * @param description tool description
   * @param parametersSchema schema map
   * @param sensitive sensitivity flag
   */
  public ToolDefinition {
    id = requireText(id, "id");
    description = normalize(description);
    parametersSchema = immutableSchema(parametersSchema);
  }

  private static Map<String, Object> immutableSchema(Map<String, Object> schema) {
    if (schema == null || schema.isEmpty()) {
      return Map.of();
    }
    return Map.copyOf(new LinkedHashMap<>(schema));
  }
}
