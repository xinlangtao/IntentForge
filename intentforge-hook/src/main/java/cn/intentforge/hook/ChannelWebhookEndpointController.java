package cn.intentforge.hook;

import cn.intentforge.channel.ChannelInboundProcessingResult;
import cn.intentforge.channel.ChannelInboundProcessor;
import cn.intentforge.channel.ChannelType;
import cn.intentforge.channel.ChannelWebhookRequest;
import java.util.Objects;

/**
 * Transport-neutral controller for generic channel webhook endpoints.
 *
 * @since 1.0.0
 */
public final class ChannelWebhookEndpointController {
  private final HookAccountRegistry accountRegistry;
  private final ChannelInboundProcessor inboundProcessor;

  /**
   * Creates one controller with the supplied account registry and inbound processor.
   *
   * @param accountRegistry hook-visible channel accounts
   * @param inboundProcessor inbound runtime pipeline
   */
  public ChannelWebhookEndpointController(HookAccountRegistry accountRegistry, ChannelInboundProcessor inboundProcessor) {
    this.accountRegistry = Objects.requireNonNull(accountRegistry, "accountRegistry must not be null");
    this.inboundProcessor = Objects.requireNonNull(inboundProcessor, "inboundProcessor must not be null");
  }

  /**
   * Handles one generic channel webhook request after the transport resolves the path variables.
   *
   * @param channelType channel type encoded in the route
   * @param accountId account identifier encoded in the route
   * @param request normalized webhook request
   * @return processing result including the HTTP-style acknowledgement response
   */
  public ChannelInboundProcessingResult handle(ChannelType channelType, String accountId, ChannelWebhookRequest request) {
    Objects.requireNonNull(channelType, "channelType must not be null");
    Objects.requireNonNull(accountId, "accountId must not be null");
    Objects.requireNonNull(request, "request must not be null");
    return inboundProcessor.process(
        accountRegistry.find(channelType, accountId)
            .orElseThrow(() -> new HookEndpointException(404, "hook account not found")),
        request);
  }
}
