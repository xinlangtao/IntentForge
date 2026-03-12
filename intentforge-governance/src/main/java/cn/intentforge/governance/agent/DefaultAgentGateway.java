package cn.intentforge.governance.agent;

import cn.intentforge.agent.core.AgentDescriptor;
import cn.intentforge.agent.core.AgentExecutionException;
import cn.intentforge.agent.core.AgentExecutor;
import cn.intentforge.agent.core.AgentGateway;
import cn.intentforge.agent.core.AgentRunGateway;
import cn.intentforge.agent.core.AgentRunResult;
import cn.intentforge.agent.core.AgentRunSnapshot;
import cn.intentforge.agent.core.AgentRunStatus;
import cn.intentforge.agent.core.AgentTask;
import cn.intentforge.model.provider.registry.ModelProviderRegistry;
import cn.intentforge.model.registry.ModelManager;
import cn.intentforge.prompt.registry.PromptManager;
import cn.intentforge.session.registry.SessionManager;
import cn.intentforge.space.SpaceResolver;
import cn.intentforge.tool.core.gateway.ToolGateway;
import java.util.List;
import java.util.Objects;

/**
 * Synchronous compatibility gateway built on top of the event-driven run gateway.
 */
public final class DefaultAgentGateway implements AgentGateway {
  private final AgentRunGateway agentRunGateway;
  private final List<AgentDescriptor> descriptors;

  /**
   * Creates the gateway with required runtime collaborators.
   *
   * @param sessionManager session manager
   * @param spaceResolver space resolver
   * @param promptManager prompt manager
   * @param modelManager model manager
   * @param modelProviderRegistry model provider registry
   * @param toolGateway tool gateway
   * @param agentRouter route selector
   * @param executors available agent executors
   */
  public DefaultAgentGateway(
      SessionManager sessionManager,
      SpaceResolver spaceResolver,
      PromptManager promptManager,
      ModelManager modelManager,
      ModelProviderRegistry modelProviderRegistry,
      ToolGateway toolGateway,
      AgentRouter agentRouter,
      List<AgentExecutor> executors
  ) {
    this(
        new DefaultAgentRunGateway(
            sessionManager,
            spaceResolver,
            promptManager,
            modelManager,
            modelProviderRegistry,
            toolGateway,
            agentRouter,
            executors),
        executors);
  }

  /**
   * Creates the gateway from an existing run gateway and executor descriptors.
   *
   * @param agentRunGateway event-driven run gateway
   * @param executors available agent executors
   */
  public DefaultAgentGateway(AgentRunGateway agentRunGateway, List<AgentExecutor> executors) {
    this.agentRunGateway = Objects.requireNonNull(agentRunGateway, "agentRunGateway must not be null");
    List<AgentExecutor> nonNullExecutors = List.copyOf(Objects.requireNonNull(executors, "executors must not be null"));
    this.descriptors = nonNullExecutors.stream().map(AgentExecutor::descriptor).toList();
  }

  /**
   * Executes one task synchronously by auto-resuming the event-driven run until terminal state.
   *
   * @param task task request
   * @return routed execution result
   */
  @Override
  public AgentRunResult execute(AgentTask task) {
    AgentRunSnapshot snapshot = agentRunGateway.start(task);
    while (snapshot.status() == AgentRunStatus.AWAITING_USER) {
      snapshot = agentRunGateway.resume(snapshot.runId(), "");
    }
    if (snapshot.status() == AgentRunStatus.FAILED) {
      throw new AgentExecutionException(
          snapshot.awaitingReason() == null ? "agent run failed: " + snapshot.runId() : snapshot.awaitingReason());
    }
    if (snapshot.status() == AgentRunStatus.CANCELLED) {
      throw new AgentExecutionException(
          snapshot.awaitingReason() == null ? "agent run cancelled: " + snapshot.runId() : snapshot.awaitingReason());
    }
    String summary = snapshot.state().decisions().isEmpty()
        ? "agent execution completed"
        : snapshot.state().decisions().getLast().summary();
    return new AgentRunResult(
        snapshot.contextPack(),
        snapshot.route(),
        snapshot.state().plan(),
        snapshot.state().decisions(),
        snapshot.state().artifacts(),
        snapshot.state().toolCalls(),
        summary);
  }

  /**
   * Lists currently registered agent descriptors.
   *
   * @return immutable descriptor list
   */
  @Override
  public List<AgentDescriptor> listAgents() {
    return descriptors;
  }
}
