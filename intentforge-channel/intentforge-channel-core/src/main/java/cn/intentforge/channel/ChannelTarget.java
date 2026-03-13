package cn.intentforge.channel;

import static cn.intentforge.common.util.ValidationSupport.immutableStringMap;
import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Represents the normalized destination of one channel interaction.
 *
 * @param accountId channel account identifier
 * @param conversationId conversation or room identifier
 * @param threadId optional thread identifier
 * @param recipientId optional direct recipient identifier
 * @param attributes transport-specific address attributes
 * @since 1.0.0
 */
public record ChannelTarget(
    String accountId,
    String conversationId,
    String threadId,
    String recipientId,
    Map<String, String> attributes
) implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Creates one validated immutable target.
   */
  public ChannelTarget {
    accountId = requireText(accountId, "accountId");
    conversationId = requireText(conversationId, "conversationId");
    threadId = normalizeOptional(threadId);
    recipientId = normalizeOptional(recipientId);
    attributes = immutableStringMap(attributes, "attributes");
  }
}
