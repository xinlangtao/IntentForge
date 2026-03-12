package cn.intentforge.tool.connectors;

import cn.intentforge.tool.connectors.flow.FlowAskToolHandler;
import cn.intentforge.tool.connectors.flow.FlowTaskDispatchToolHandler;
import cn.intentforge.tool.connectors.flow.FlowTodoReadToolHandler;
import cn.intentforge.tool.connectors.flow.FlowTodoWriteToolHandler;
import cn.intentforge.tool.connectors.web.WebFetchToolHandler;
import cn.intentforge.tool.connectors.web.WebSearchToolHandler;
import cn.intentforge.tool.core.model.ToolDefinition;
import cn.intentforge.tool.core.registry.ToolRegistration;
import cn.intentforge.tool.core.spi.ToolPlugin;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Provides connector tools including web and flow collaboration tools.
 */
public final class ConnectorToolPlugin implements ToolPlugin {
  /**
   * Tool id: web fetch.
   */
  public static final String TOOL_WEB_FETCH = "intentforge.web.fetch";
  /**
   * Tool id: web search.
   */
  public static final String TOOL_WEB_SEARCH = "intentforge.web.search";
  /**
   * Tool id: runtime environment read.
   */
  public static final String TOOL_RUNTIME_ENVIRONMENT_READ = "intentforge.runtime.environment.read";
  /**
   * Tool id: interactive ask.
   */
  public static final String TOOL_FLOW_ASK = "intentforge.flow.ask";
  /**
   * Tool id: todo write.
   */
  public static final String TOOL_FLOW_TODO_WRITE = "intentforge.flow.todo.write";
  /**
   * Tool id: todo read.
   */
  public static final String TOOL_FLOW_TODO_READ = "intentforge.flow.todo.read";
  /**
   * Tool id: task dispatch.
   */
  public static final String TOOL_FLOW_TASK_DISPATCH = "intentforge.flow.task.dispatch";

  @Override
  public Collection<ToolRegistration> tools() {
    return List.of(
        new ToolRegistration(
            new ToolDefinition(
                TOOL_WEB_FETCH,
                "Fetch one HTTP resource with timeout and truncation.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "url", Map.of("type", "string"),
                        "method", Map.of("type", "string"),
                        "headers", Map.of("type", "object"),
                        "body", Map.of("type", "string"),
                        "timeoutMs", Map.of("type", "integer", "minimum", 1),
                        "maxBytes", Map.of("type", "integer", "minimum", 1),
                        "followRedirects", Map.of("type", "boolean")),
                    "required", List.of("url")),
                true),
            new WebFetchToolHandler()),
        new ToolRegistration(
            new ToolDefinition(
                TOOL_WEB_SEARCH,
                "Run web search via pluggable search provider.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "q", Map.of("type", "string"),
                        "provider", Map.of("type", "string"),
                        "limit", Map.of("type", "integer", "minimum", 1, "maximum", 20),
                        "timeoutMs", Map.of("type", "integer", "minimum", 1)),
                    "required", List.of("q")),
                true),
            new WebSearchToolHandler()),
        new ToolRegistration(
            new ToolDefinition(
                TOOL_RUNTIME_ENVIRONMENT_READ,
                "Read normalized runtime environment details including OS, shell, terminal, and IDE launch entries.",
                Map.of("type", "object", "properties", Map.of()),
                false),
            new RuntimeEnvironmentToolHandler()),
        new ToolRegistration(
            new ToolDefinition(
                TOOL_FLOW_ASK,
                "Request user confirmation/input from runtime.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "question", Map.of("type", "string"),
                        "options", Map.of("type", "array", "items", Map.of("type", "string"))),
                    "required", List.of("question")),
                false),
            new FlowAskToolHandler()),
        new ToolRegistration(
            new ToolDefinition(
                TOOL_FLOW_TODO_WRITE,
                "Write todo items into runtime context.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "items", Map.of(
                            "description", "String or string list",
                            "type", "array",
                            "items", Map.of("type", "string")),
                        "append", Map.of("type", "boolean")),
                    "required", List.of("items")),
                false),
            new FlowTodoWriteToolHandler()),
        new ToolRegistration(
            new ToolDefinition(
                TOOL_FLOW_TODO_READ,
                "Read todo items from runtime context.",
                Map.of("type", "object", "properties", Map.of()),
                false),
            new FlowTodoReadToolHandler()),
        new ToolRegistration(
            new ToolDefinition(
                TOOL_FLOW_TASK_DISPATCH,
                "Dispatch task to another worker/runtime.",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "dispatchId", Map.of("type", "string"),
                        "target", Map.of("type", "string"),
                        "task", Map.of("type", "string"),
                        "payload", Map.of("type", "object")),
                    "required", List.of("target", "task")),
                true),
            new FlowTaskDispatchToolHandler()));
  }
}
