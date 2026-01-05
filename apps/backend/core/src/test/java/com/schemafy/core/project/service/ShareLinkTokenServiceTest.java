package com.schemafy.core.project.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShareLinkTokenService 단위 테스트")
class ShareLinkTokenServiceTest {

  private ShareLinkTokenService tokenService;
  private static final String TEST_PEPPER = "test-pepper-value";

  @BeforeEach
  void setUp() {
    tokenService = new ShareLinkTokenService(TEST_PEPPER);
  }

  @Test
  @DisplayName("토큰 생성에 성공한다")
  void generateToken_ReturnsNonEmptyToken() {
    String token = tokenService.generateToken();

    assertThat(token)
        .isNotNull()
        .isNotEmpty();
  }

  @Test
  @DisplayName("생성된 토큰은 URL-safe Base64 형식이다")
  void generateToken_ReturnsUrlSafeBase64() {
    String token = tokenService.generateToken();

    assertThat(token).matches("^[A-Za-z0-9_-]+$");
  }

  @Test
  @DisplayName("매번 다른 토큰이 생성된다")
  void generateToken_ReturnsDifferentTokensEachTime() {
    String token1 = tokenService.generateToken();
    String token2 = tokenService.generateToken();

    assertThat(token1).isNotEqualTo(token2);
  }

  @Test
  @DisplayName("토큰 해싱에 성공한다")
  void hashToken_ReturnsNonEmptyHash() {
    String token = "test-token";

    byte[] hash = tokenService.hashToken(token);

    assertThat(hash)
        .isNotNull()
        .hasSize(32); // SHA-256 produces 32 bytes
  }

  @Test
  @DisplayName("동일한 토큰은 동일한 해시를 반환한다")
  void hashToken_ReturnsSameHashForSameToken() {
    String token = "test-token";

    byte[] hash1 = tokenService.hashToken(token);
    byte[] hash2 = tokenService.hashToken(token);

    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  @DisplayName("다른 토큰은 다른 해시를 반환한다")
  void hashToken_ReturnsDifferentHashForDifferentToken() {
    String token1 = "test-token-1";
    String token2 = "test-token-2";

    byte[] hash1 = tokenService.hashToken(token1);
    byte[] hash2 = tokenService.hashToken(token2);

    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  @DisplayName("다른 pepper를 사용하면 다른 해시를 반환한다")
  void hashToken_ReturnsDifferentHashWithDifferentPepper() {
    String token = "test-token";
    ShareLinkTokenService anotherService = new ShareLinkTokenService(
        "different-pepper");

    byte[] hash1 = tokenService.hashToken(token);
    byte[] hash2 = anotherService.hashToken(token);

    assertThat(hash1).isNotEqualTo(hash2);
  }

}
