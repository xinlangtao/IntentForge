package cn.intentforge.tool.connectors;

import cn.intentforge.tool.core.registry.InMemoryToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConnectorToolPluginTest {
  @Test
  void shouldLoadConnectorPluginThroughServiceLoader() {
    InMemoryToolRegistry registry = new InMemoryToolRegistry();
    registry.loadPlugins();
    Assertions.assertTrue(registry.find(ConnectorToolPlugin.TOOL_WEB_FETCH).isPresent());
    Assertions.assertTrue(registry.find(ConnectorToolPlugin.TOOL_WEB_SEARCH).isPresent());
    Assertions.assertTrue(registry.find(ConnectorToolPlugin.TOOL_RUNTIME_ENVIRONMENT_READ).isPresent());
    Assertions.assertTrue(registry.find(ConnectorToolPlugin.TOOL_FLOW_TASK_DISPATCH).isPresent());
  }
}
