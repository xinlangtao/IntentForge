package cn.intentforge.channel.wecom.crypto;

import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Calculates and validates WeCom callback signatures.
 *
 * @since 1.0.0
 */
public final class WeComSignatureVerifier {
  private final String token;

  /**
   * Creates one verifier bound to the provided callback token.
   *
   * @param token callback token
   */
  public WeComSignatureVerifier(String token) {
    this.token = requireText(token, "token");
  }

  /**
   * Calculates the WeCom callback signature for one encrypted payload.
   *
   * @param timestamp callback timestamp
   * @param nonce callback nonce
   * @param encryptedPayload encrypted callback body
   * @return hex-encoded SHA-1 signature
   */
  public String signature(String timestamp, String nonce, String encryptedPayload) {
    String[] parts = {
        token,
        requireText(timestamp, "timestamp"),
        requireText(nonce, "nonce"),
        requireText(encryptedPayload, "encryptedPayload")
    };
    Arrays.sort(parts);
    return sha1(String.join("", parts));
  }

  /**
   * Verifies one provided callback signature.
   *
   * @param providedSignature provided signature
   * @param timestamp callback timestamp
   * @param nonce callback nonce
   * @param encryptedPayload encrypted callback body
   */
  public void verify(String providedSignature, String timestamp, String nonce, String encryptedPayload) {
    String expected = signature(timestamp, nonce, encryptedPayload);
    String normalizedProvided = requireText(providedSignature, "providedSignature");
    if (!expected.equalsIgnoreCase(normalizedProvided)) {
      throw new IllegalArgumentException("invalid WeCom callback signature");
    }
  }

  private static String sha1(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(hash.length * 2);
      for (byte item : hash) {
        builder.append(String.format("%02x", item));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-1 is not available", exception);
    }
  }
}
