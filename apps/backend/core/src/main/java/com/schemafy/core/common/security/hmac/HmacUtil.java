package com.schemafy.core.common.security.hmac;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HmacUtil {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String SHA256_ALGORITHM = "SHA-256";
  private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

  public static SecretKey createSecretKey(String secret) {
    return new SecretKeySpec(
        secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
  }

  public static String computeHmac(SecretKey key, String data) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(key);
      byte[] rawHmac = mac
          .doFinal(data.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(rawHmac);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to compute HMAC-SHA256", e);
    }
  }

  public static String computeBodyHash(byte[] body) {
    try {
      MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
      byte[] hash = digest.digest(body != null ? body : new byte[0]);
      return bytesToHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  public static String buildHttpCanonicalString(String method,
      String path, String timestamp, String nonce, String bodyHash) {
    return method + "\n"
        + path + "\n"
        + timestamp + "\n"
        + nonce + "\n"
        + bodyHash + "\n";
  }

  public static boolean verifySignature(String expected, String actual) {
    if (expected == null || actual == null) {
      return false;
    }
    byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
    byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
    return MessageDigest.isEqual(expectedBytes, actualBytes);
  }

  private static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int v = bytes[i] & 0xFF;
      hexChars[i * 2] = HEX_CHARS[v >>> 4];
      hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
    }
    return new String(hexChars);
  }

}
