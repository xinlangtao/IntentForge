package cn.intentforge.channel.wecom.crypto;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.util.Map;

/**
 * Represents one encrypted WeCom callback payload plus its signature envelope.
 *
 * @param encrypt encrypted payload text
 * @param messageSignature signature calculated for the encrypted payload
 * @param timestamp callback timestamp
 * @param nonce callback nonce
 * @since 1.0.0
 */
public record WeComEncryptedResponse(
    String encrypt,
    String messageSignature,
    String timestamp,
    String nonce
) {
  /**
   * Creates one validated encrypted response envelope.
   */
  public WeComEncryptedResponse {
    encrypt = requireText(encrypt, "encrypt");
    messageSignature = requireText(messageSignature, "messageSignature");
    timestamp = requireText(timestamp, "timestamp");
    nonce = requireText(nonce, "nonce");
  }

  /**
   * Converts this envelope into the JSON response body expected by the WeCom callback protocol.
   *
   * @return immutable JSON body fields
   */
  public Map<String, String> toResponseBody() {
    return Map.of(
        "encrypt", encrypt,
        "msgsignature", messageSignature,
        "timestamp", timestamp,
        "nonce", nonce);
  }
}
