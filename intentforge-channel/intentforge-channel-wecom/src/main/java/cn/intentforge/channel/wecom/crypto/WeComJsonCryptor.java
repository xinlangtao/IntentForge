package cn.intentforge.channel.wecom.crypto;

import static cn.intentforge.common.util.ValidationSupport.normalizeOptional;
import static cn.intentforge.common.util.ValidationSupport.requireText;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypts and decrypts WeCom intelligent-robot callback payloads in JSON mode.
 *
 * @since 1.0.0
 */
public final class WeComJsonCryptor {
  private static final int NETWORK_ORDER_LENGTH_BYTES = 4;
  private static final int RANDOM_PREFIX_BYTES = 16;
  private static final int BLOCK_SIZE = 32;

  private final WeComSignatureVerifier signatureVerifier;
  private final SecretKeySpec secretKeySpec;
  private final IvParameterSpec ivParameterSpec;
  private final String receiveId;
  private final SecureRandom secureRandom;

  /**
   * Creates one cryptor bound to the provided callback credentials.
   *
   * @param token callback token
   * @param encodingAesKey callback encoding AES key
   * @param receiveId optional receive-id check
   */
  public WeComJsonCryptor(String token, String encodingAesKey, String receiveId) {
    this(token, encodingAesKey, receiveId, new SecureRandom());
  }

  WeComJsonCryptor(String token, String encodingAesKey, String receiveId, SecureRandom secureRandom) {
    this.signatureVerifier = new WeComSignatureVerifier(token);
    byte[] aesKey = decodeAesKey(encodingAesKey);
    this.secretKeySpec = new SecretKeySpec(aesKey, "AES");
    this.ivParameterSpec = new IvParameterSpec(Arrays.copyOf(aesKey, RANDOM_PREFIX_BYTES));
    this.receiveId = normalizeOptional(receiveId);
    this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
  }

  /**
   * Encrypts one plain callback payload into a signed WeCom response envelope.
   *
   * @param plainText plain callback payload
   * @param timestamp callback timestamp
   * @param nonce callback nonce
   * @return encrypted response envelope
   */
  public WeComEncryptedResponse encrypt(String plainText, String timestamp, String nonce) {
    String nonBlankPlainText = requireText(plainText, "plainText");
    String nonBlankTimestamp = requireText(timestamp, "timestamp");
    String nonBlankNonce = requireText(nonce, "nonce");
    byte[] payload = plainTextBytes(nonBlankPlainText);
    String encrypted = Base64.getEncoder().encodeToString(doCipher(Cipher.ENCRYPT_MODE, payload));
    return new WeComEncryptedResponse(
        encrypted,
        signatureVerifier.signature(nonBlankTimestamp, nonBlankNonce, encrypted),
        nonBlankTimestamp,
        nonBlankNonce);
  }

  /**
   * Verifies the callback signature and decrypts the encrypted callback payload.
   *
   * @param signature callback signature
   * @param timestamp callback timestamp
   * @param nonce callback nonce
   * @param encryptedPayload encrypted callback payload
   * @return decrypted plain payload
   */
  public String verifyAndDecrypt(String signature, String timestamp, String nonce, String encryptedPayload) {
    String nonBlankEncryptedPayload = requireText(encryptedPayload, "encryptedPayload");
    signatureVerifier.verify(signature, timestamp, nonce, nonBlankEncryptedPayload);
    byte[] decrypted = doCipher(Cipher.DECRYPT_MODE, Base64.getDecoder().decode(nonBlankEncryptedPayload));
    byte[] unpadded = removePadding(decrypted);
    ByteBuffer buffer = ByteBuffer.wrap(unpadded);
    buffer.position(RANDOM_PREFIX_BYTES);
    int messageLength = buffer.getInt();
    if (messageLength < 0 || messageLength > buffer.remaining()) {
      throw new IllegalArgumentException("invalid WeCom encrypted payload length");
    }
    byte[] messageBytes = new byte[messageLength];
    buffer.get(messageBytes);
    byte[] receiveIdBytes = new byte[buffer.remaining()];
    buffer.get(receiveIdBytes);
    validateReceiveId(receiveIdBytes);
    return new String(messageBytes, StandardCharsets.UTF_8);
  }

  private byte[] plainTextBytes(String plainText) {
    byte[] randomPrefix = new byte[RANDOM_PREFIX_BYTES];
    secureRandom.nextBytes(randomPrefix);
    byte[] messageBytes = plainText.getBytes(StandardCharsets.UTF_8);
    byte[] receiveIdBytes = receiveId == null ? new byte[0] : receiveId.getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.allocate(
        RANDOM_PREFIX_BYTES + NETWORK_ORDER_LENGTH_BYTES + messageBytes.length + receiveIdBytes.length);
    buffer.put(randomPrefix);
    buffer.putInt(messageBytes.length);
    buffer.put(messageBytes);
    buffer.put(receiveIdBytes);
    return addPadding(buffer.array());
  }

  private byte[] doCipher(int mode, byte[] bytes) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
      cipher.init(mode, secretKeySpec, ivParameterSpec);
      return cipher.doFinal(bytes);
    } catch (GeneralSecurityException exception) {
      throw new IllegalArgumentException("failed to process WeCom encrypted payload", exception);
    }
  }

  private void validateReceiveId(byte[] receiveIdBytes) {
    if (receiveId == null) {
      return;
    }
    String actual = new String(receiveIdBytes, StandardCharsets.UTF_8);
    if (!receiveId.equals(actual)) {
      throw new IllegalArgumentException("unexpected WeCom receiveId");
    }
  }

  private static byte[] decodeAesKey(String encodingAesKey) {
    String normalizedKey = requireText(encodingAesKey, "encodingAesKey");
    try {
      byte[] aesKey = Base64.getDecoder().decode(normalizedKey + "=");
      if (aesKey.length != 32) {
        throw new IllegalArgumentException("encodingAesKey must decode to 32 bytes");
      }
      return aesKey;
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("invalid WeCom encodingAesKey", exception);
    }
  }

  private static byte[] addPadding(byte[] value) {
    int paddingLength = BLOCK_SIZE - (value.length % BLOCK_SIZE);
    if (paddingLength == 0) {
      paddingLength = BLOCK_SIZE;
    }
    byte[] padded = Arrays.copyOf(value, value.length + paddingLength);
    Arrays.fill(padded, value.length, padded.length, (byte) paddingLength);
    return padded;
  }

  private static byte[] removePadding(byte[] value) {
    if (value.length == 0) {
      throw new IllegalArgumentException("invalid padded value");
    }
    int paddingLength = value[value.length - 1] & 0xFF;
    if (paddingLength < 1 || paddingLength > BLOCK_SIZE || paddingLength > value.length) {
      throw new IllegalArgumentException("invalid WeCom padding");
    }
    for (int index = value.length - paddingLength; index < value.length; index += 1) {
      if ((value[index] & 0xFF) != paddingLength) {
        throw new IllegalArgumentException("invalid WeCom padding bytes");
      }
    }
    return Arrays.copyOf(value, value.length - paddingLength);
  }
}
