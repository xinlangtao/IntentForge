package cn.intentforge.channel.wecom.crypto;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WeComJsonCryptorTest {
  private static final String TOKEN = "robot-token";
  private static final String ENCODING_AES_KEY = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
  private static final String RECEIVE_ID = "robot-receive-id";

  @Test
  void shouldVerifyAndDecryptEchoChallenge() {
    WeComJsonCryptor cryptor = new WeComJsonCryptor(TOKEN, ENCODING_AES_KEY, RECEIVE_ID);

    WeComEncryptedResponse encrypted = cryptor.encrypt("verify-me", "1710000000", "nonce-1");
    String decrypted = cryptor.verifyAndDecrypt(
        encrypted.messageSignature(),
        encrypted.timestamp(),
        encrypted.nonce(),
        encrypted.encrypt());

    Assertions.assertEquals("verify-me", decrypted);
  }

  @Test
  void shouldRejectInvalidSignature() {
    WeComJsonCryptor cryptor = new WeComJsonCryptor(TOKEN, ENCODING_AES_KEY, RECEIVE_ID);
    WeComEncryptedResponse encrypted = cryptor.encrypt("{\"msgtype\":\"text\"}", "1710000000", "nonce-1");

    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> cryptor.verifyAndDecrypt(
            "invalid-signature",
            encrypted.timestamp(),
            encrypted.nonce(),
            encrypted.encrypt()));
  }

  @Test
  void shouldEncryptCallbackAcknowledgement() {
    WeComJsonCryptor cryptor = new WeComJsonCryptor(TOKEN, ENCODING_AES_KEY, RECEIVE_ID);

    WeComEncryptedResponse encrypted = cryptor.encrypt("{\"ack\":\"success\"}", "1710000001", "nonce-2");

    Assertions.assertEquals(
        "{\"ack\":\"success\"}",
        cryptor.verifyAndDecrypt(
            encrypted.messageSignature(),
            encrypted.timestamp(),
            encrypted.nonce(),
            encrypted.encrypt()));
    Assertions.assertEquals(
        Map.of("encrypt", encrypted.encrypt(), "msgsignature", encrypted.messageSignature(), "timestamp", encrypted.timestamp(), "nonce", encrypted.nonce()),
        encrypted.toResponseBody());
  }
}
