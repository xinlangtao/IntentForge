package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents the access control decision for one inbound channel message.
 *
 * @param allowed whether the message can continue
 * @param reason optional rejection or trace reason
 * @since 1.0.0
 */
public record ChannelAccessDecision(
    boolean allowed,
    String reason
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one normalized decision.
   */
  public ChannelAccessDecision {
    reason = normalizeOptional(reason);
  }
}
