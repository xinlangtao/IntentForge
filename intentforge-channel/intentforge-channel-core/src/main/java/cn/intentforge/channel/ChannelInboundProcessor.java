package cn.intentforge.channel;

/**
 * Processes inbound webhook requests through parsing, access control, and route resolution.
 *
 * @since 1.0.0
 */
public interface ChannelInboundProcessor {
  /**
   * Processes one inbound webhook request for the provided account profile.
   *
   * @param accountProfile channel account profile
   * @param request normalized webhook request
   * @return aggregated inbound processing result
   */
  ChannelInboundProcessingResult process(ChannelAccountProfile accountProfile, ChannelWebhookRequest request);
}
