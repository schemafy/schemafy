package com.schemafy.domain.ulid.application.service;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.ulid.exception.UlidErrorCode;

public class UlidGenerator {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final char[] BASE32_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
      .toCharArray();

  private static long lastTimestamp = -1L;
  private static final byte[] lastRandomBytes = new byte[10];

  public static synchronized String generate() {
    long timestamp = Instant.now().toEpochMilli();

    if (timestamp > lastTimestamp) {
      lastTimestamp = timestamp;
      SECURE_RANDOM.nextBytes(lastRandomBytes);
    } else {
      incrementRandomBytes(lastRandomBytes);
      timestamp = lastTimestamp;
    }

    byte[] timestampBytes = new byte[6];
    timestampBytes[0] = (byte) ((timestamp >>> 40) & 0xFF);
    timestampBytes[1] = (byte) ((timestamp >>> 32) & 0xFF);
    timestampBytes[2] = (byte) ((timestamp >>> 24) & 0xFF);
    timestampBytes[3] = (byte) ((timestamp >>> 16) & 0xFF);
    timestampBytes[4] = (byte) ((timestamp >>> 8) & 0xFF);
    timestampBytes[5] = (byte) (timestamp & 0xFF);

    byte[] ulidBytes = new byte[16];
    System.arraycopy(timestampBytes, 0, ulidBytes, 0, 6);
    System.arraycopy(lastRandomBytes, 0, ulidBytes, 6, 10);

    return encodeBase32(ulidBytes);
  }

  private static void incrementRandomBytes(byte[] randomBytes) {
    for (int i = randomBytes.length - 1; i >= 0; i--) {
      int value = (randomBytes[i] & 0xFF) + 1;
      randomBytes[i] = (byte) value;
      if (value <= 0xFF) {
        return;
      }
    }

    long timestamp;
    do {
      timestamp = Instant.now().toEpochMilli();
    } while (timestamp <= lastTimestamp);
    lastTimestamp = timestamp;
    SECURE_RANDOM.nextBytes(randomBytes);
  }

  private static String encodeBase32(byte[] data) {
    StringBuilder result = new StringBuilder();

    int buffer = 0;
    int bufferLength = 0;

    for (byte b : data) {
      buffer = (buffer << 8) | (b & 0xFF);
      bufferLength += 8;

      while (bufferLength >= 5) {
        result.append(BASE32_ALPHABET[(buffer >>> (bufferLength - 5))
            & 0x1F]);
        bufferLength -= 5;
      }
    }

    if (bufferLength > 0) {
      result.append(
          BASE32_ALPHABET[(buffer << (5 - bufferLength)) & 0x1F]);
    }

    return result.toString();
  }

  public static long extractTimestamp(String ulid) {
    if (ulid == null || ulid.length() != 26) {
      throw new DomainException(UlidErrorCode.INVALID_VALUE, "Invalid ULID format");
    }

    byte[] data = decodeBase32(ulid);

    long timestamp = 0;
    for (int i = 0; i < 6; i++) {
      timestamp = (timestamp << 8) | (data[i] & 0xFF);
    }

    return timestamp;
  }

  private static byte[] decodeBase32(String base32) {
    int[] charToIndex = new int[256];
    for (int i = 0; i < charToIndex.length; i++) {
      charToIndex[i] = -1;
    }

    for (int i = 0; i < BASE32_ALPHABET.length; i++) {
      charToIndex[BASE32_ALPHABET[i]] = i;
    }

    for (int i = 0; i < BASE32_ALPHABET.length; i++) {
      char lower = Character.toLowerCase(BASE32_ALPHABET[i]);
      char upper = Character.toUpperCase(BASE32_ALPHABET[i]);
      if (charToIndex[lower] == -1)
        charToIndex[lower] = i;
      if (charToIndex[upper] == -1)
        charToIndex[upper] = i;
    }

    ByteBuffer buffer = ByteBuffer.allocate(16);
    int bitBuffer = 0;
    int bitCount = 0;

    for (char c : base32.toCharArray()) {
      int index = charToIndex[c];
      if (index == -1) {
        throw new DomainException(UlidErrorCode.INVALID_VALUE,
            "Invalid Base32 character: " + c);
      }

      bitBuffer = (bitBuffer << 5) | index;
      bitCount += 5;

      while (bitCount >= 8) {
        buffer.put((byte) ((bitBuffer >>> (bitCount - 8)) & 0xFF));
        bitCount -= 8;
      }
    }

    return buffer.array();
  }

}
