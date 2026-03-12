package cn.intentforge.boot.local;

import cn.intentforge.agent.core.AgentGateway;
import cn.intentforge.agent.core.AgentRunGateway;
import cn.intentforge.config.RuntimeCatalog;
import cn.intentforge.model.local.plugin.DirectoryModelPluginManager;
import cn.intentforge.model.provider.local.plugin.DirectoryModelProviderPluginManager;
import cn.intentforge.model.provider.registry.ModelProviderRegistry;
import cn.intentforge.model.registry.ModelManager;
import cn.intentforge.prompt.local.plugin.DirectoryPromptPluginManager;
import cn.intentforge.prompt.registry.PromptManager;
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.space.SpaceRegistry;
import cn.intentforge.space.SpaceResolver;
import cn.intentforge.tool.core.gateway.ToolGateway;
import cn.intentforge.tool.core.local.plugin.DirectoryToolPluginManager;
import cn.intentforge.tool.core.permission.ToolPermissionPolicy;
import cn.intentforge.tool.core.registry.ToolRegistry;

/**
 * Local runtime wiring for prompt, model, provider, tool, session, and space assets.
 *
 * @param runtimeCatalog discovered runtime implementation catalog
 * @param promptManager prompt manager implementation in use
 * @param promptPluginManager prompt plugin directory manager
 * @param modelManager model manager implementation in use
 * @param modelPluginManager model plugin directory manager
 * @param providerRegistry model provider registry implementation in use
 * @param providerPluginManager model provider plugin directory manager
 * @param toolRegistry tool registry implementation in use
 * @param toolPluginManager tool plugin directory manager
 * @param toolPermissionPolicy tool permission policy
 * @param toolGateway tool execution gateway
 * @param agentGateway agent execution gateway
 * @param agentRunGateway event-driven agent run gateway
 * @param sessionManager session manager implementation in use
 * @param spaceRegistry space registry implementation in use
 * @param spaceResolver space inheritance resolver
 */
public record AiAssetLocalRuntime(
    RuntimeCatalog runtimeCatalog,
    PromptManager promptManager,
    DirectoryPromptPluginManager promptPluginManager,
    ModelManager modelManager,
    DirectoryModelPluginManager modelPluginManager,
    ModelProviderRegistry providerRegistry,
    DirectoryModelProviderPluginManager providerPluginManager,
    ToolRegistry toolRegistry,
    DirectoryToolPluginManager toolPluginManager,
    ToolPermissionPolicy toolPermissionPolicy,
    ToolGateway toolGateway,
    AgentGateway agentGateway,
    AgentRunGateway agentRunGateway,
    SessionManager sessionManager,
    SpaceRegistry spaceRegistry,
    SpaceResolver spaceResolver
) {
}
