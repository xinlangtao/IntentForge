package cn.intentforge.tool.mcp;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP remote tool descriptor.
 *
 * @param id remote tool identifier
 * @param description remote description
 * @param inputSchema remote input schema
 * @param sensitive whether this tool should be marked as sensitive locally
 */
public record McpRemoteTool(
    String id,
    String description,
    Map<String, Object> inputSchema,
    boolean sensitive
) {
  /**
   * Creates one remote tool descriptor.
   *
   * @param id remote tool identifier
   * @param description remote description
   * @param inputSchema remote input schema
   * @param sensitive sensitive flag
   */
  public McpRemoteTool {
    id = normalize(id);
    if (id == null) {
      throw new IllegalArgumentException("id must not be blank");
    }
    description = normalize(description);
    inputSchema = inputSchema == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(inputSchema));
  }
}
