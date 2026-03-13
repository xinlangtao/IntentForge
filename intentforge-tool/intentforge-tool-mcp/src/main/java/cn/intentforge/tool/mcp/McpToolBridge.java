package cn.intentforge.tool.mcp;

import static cn.intentforge.common.util.ValidationSupport.normalize;

import cn.intentforge.tool.core.model.ToolCallResult;
import cn.intentforge.tool.core.model.ToolDefinition;
import cn.intentforge.tool.core.registry.ToolRegistration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Bridges remote MCP tools into local {@link ToolRegistration} objects.
 */
public final class McpToolBridge {
  private final McpClient client;
  private final String localToolPrefix;

  /**
   * Creates bridge with default local id prefix {@code intentforge.mcp}.
   *
   * @param client MCP client
   */
  public McpToolBridge(McpClient client) {
    this(client, "intentforge.mcp");
  }

  /**
   * Creates bridge.
   *
   * @param client MCP client
   * @param localToolPrefix local tool prefix
   */
  public McpToolBridge(McpClient client, String localToolPrefix) {
    this.client = Objects.requireNonNull(client, "client must not be null");
    String normalizedPrefix = normalize(localToolPrefix);
    this.localToolPrefix = normalizedPrefix == null ? "intentforge.mcp" : normalizedPrefix;
  }

  /**
   * Bridges all currently available remote tools.
   *
   * @return local registrations for MCP tools
   */
  public Collection<ToolRegistration> bridgeTools() {
    List<McpRemoteTool> remoteTools = client.listTools();
    if (remoteTools == null || remoteTools.isEmpty()) {
      return List.of();
    }
    return remoteTools.stream().map(this::bridgeTool).toList();
  }

  /**
   * Bridges one remote MCP tool.
   *
   * @param remoteTool remote tool descriptor
   * @return local tool registration
   */
  public ToolRegistration bridgeTool(McpRemoteTool remoteTool) {
    Objects.requireNonNull(remoteTool, "remoteTool must not be null");
    String localToolId = localToolId(remoteTool.id());
    String description = normalize(remoteTool.description());
    if (description == null) {
      description = "MCP bridged tool: " + remoteTool.id();
    }

    ToolDefinition definition = new ToolDefinition(
        localToolId,
        description,
        remoteTool.inputSchema(),
        remoteTool.sensitive());

    return new ToolRegistration(definition, request -> {
      McpRemoteCallResult remoteResult = client.callTool(remoteTool.id(), request.parameters(), request.context());
      return adaptResult(remoteResult, remoteTool.id());
    });
  }

  /**
   * Converts remote tool id to local tool id.
   *
   * @param remoteToolId remote tool identifier
   * @return local tool identifier
   */
  public String localToolId(String remoteToolId) {
    String normalizedRemoteId = normalize(remoteToolId);
    if (normalizedRemoteId == null) {
      throw new IllegalArgumentException("remoteToolId must not be blank");
    }
    String sanitized = normalizedRemoteId
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9._-]+", "-")
        .replaceAll("-{2,}", "-")
        .replaceAll("(^-|-$)", "");
    return localToolPrefix + "." + sanitized;
  }

  private static ToolCallResult adaptResult(McpRemoteCallResult remoteResult, String remoteToolId) {
    if (remoteResult == null) {
      return ToolCallResult.error("MCP_REMOTE_NULL", "MCP returned null result for: " + remoteToolId);
    }
    if (remoteResult.success()) {
      return ToolCallResult.success(remoteResult.output(), remoteResult.structured(), remoteResult.metadata());
    }
    String errorCode = normalize(remoteResult.errorCode());
    if (errorCode == null) {
      errorCode = "MCP_REMOTE_ERROR";
    }
    String errorMessage = normalize(remoteResult.errorMessage());
    if (errorMessage == null) {
      errorMessage = "MCP tool failed: " + remoteToolId;
    }
    return ToolCallResult.error(errorCode, errorMessage, remoteResult.metadata());
  }
}
