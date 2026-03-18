package cn.intentforge.channel.wecom.outbound;

/**
 * WeCom intelligent-robot outbound API client contract.
 *
 * @since 1.0.0
 */
public interface WeComRobotApiClient {
  /**
   * Sends one text message through the intelligent-robot outbound API.
   *
   * @param command outbound send command
   * @return send result
   */
  WeComRobotSendMessageResult sendText(WeComRobotSendMessageCommand command);
}
