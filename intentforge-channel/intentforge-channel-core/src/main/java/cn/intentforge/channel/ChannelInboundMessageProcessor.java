package cn.intentforge.channel;

import java.util.List;

/**
 * Processes already normalized inbound channel messages through access control and route resolution.
 *
 * @since 1.0.0
 */
public interface ChannelInboundMessageProcessor {
  /**
   * Processes one batch of normalized inbound messages for the provided account profile.
   *
   * @param accountProfile channel account profile
   * @param source ingress source descriptor
   * @param messages normalized inbound messages
   * @return aggregated message processing result
   */
  ChannelInboundMessageProcessingResult process(
      ChannelAccountProfile accountProfile,
      ChannelInboundSource source,
      List<ChannelInboundMessage> messages
  );
}
