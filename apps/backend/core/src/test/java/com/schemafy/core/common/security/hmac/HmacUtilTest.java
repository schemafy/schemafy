package com.schemafy.core.common.security.hmac;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacUtilTest {

  private static final String SECRET = "test-secret-key";
  private final SecretKey key = HmacUtil.createSecretKey(SECRET);

  @Test
  @DisplayName("동일한 데이터에 대해 동일한 HMAC을 생성한다")
  void computeHmacDeterministic() {
    String data = "GET\n/api/v1.0/tables\n1707820800000\nnonce-1\nbodyhash\n";
    String hmac1 = HmacUtil.computeHmac(key, data);
    String hmac2 = HmacUtil.computeHmac(key, data);
    assertThat(hmac1).isEqualTo(hmac2);
    assertThat(hmac1).hasSize(64);
  }

  @Test
  @DisplayName("다른 키로 생성한 HMAC은 다르다")
  void differentKeyProducesDifferentHmac() {
    SecretKey otherKey = HmacUtil.createSecretKey("other-secret");
    String data = "test-data";
    String hmac1 = HmacUtil.computeHmac(key, data);
    String hmac2 = HmacUtil.computeHmac(otherKey, data);
    assertThat(hmac1).isNotEqualTo(hmac2);
  }

  @Test
  @DisplayName("빈 body의 SHA-256 해시를 정확히 계산한다")
  void computeBodyHashEmpty() {
    String hash = HmacUtil.computeBodyHash(new byte[0]);
    assertThat(hash).isEqualTo(
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  @Test
  @DisplayName("null body를 빈 body처럼 처리한다")
  void computeBodyHashNull() {
    String hash = HmacUtil.computeBodyHash(null);
    assertThat(hash).isEqualTo(
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  @Test
  @DisplayName("body가 있는 경우 올바른 SHA-256 해시를 계산한다")
  void computeBodyHashWithContent() {
    String hash = HmacUtil
        .computeBodyHash("{\"name\":\"test\"}".getBytes());
    assertThat(hash).hasSize(64);
    assertThat(hash).isNotEqualTo(
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  @Test
  @DisplayName("HTTP canonical string을 올바르게 구성한다")
  void buildHttpCanonicalString() {
    String canonical = HmacUtil.buildHttpCanonicalString(
        "POST", "/api/v1.0/tables", "1707820800000", "nonce-1",
        "bodyhash");
    assertThat(canonical).isEqualTo(
        "POST\n/api/v1.0/tables\n1707820800000\nnonce-1\nbodyhash\n");
  }

  @Test
  @DisplayName("동일한 서명을 timing-safe하게 비교한다")
  void verifySignatureMatch() {
    String sig = HmacUtil.computeHmac(key, "data");
    assertThat(HmacUtil.verifySignature(sig, sig)).isTrue();
  }

  @Test
  @DisplayName("다른 서명을 거부한다")
  void verifySignatureMismatch() {
    assertThat(HmacUtil.verifySignature("abc", "def")).isFalse();
  }

  @Test
  @DisplayName("null 서명을 거부한다")
  void verifySignatureNull() {
    assertThat(HmacUtil.verifySignature(null, "abc")).isFalse();
    assertThat(HmacUtil.verifySignature("abc", null)).isFalse();
  }

}
