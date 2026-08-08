package com.schemafy.api.user.adapter.out;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.user.application.port.out.AuthTokenPort;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthTokenLocalAdapter")
class AuthTokenLocalAdapterTest {

  private static final String SUBJECT = "user@example.com";
  private static final String SECRET = "test-auth-token-secret";

  @Test
  @DisplayName("Redis 비활성화 시 회원가입 인증 토큰에 로컬 캐시를 활용한다")
  void usesLocalCacheForSignupAuthTokenWhenRedisDisabled() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AuthTokenLocalAdapterConfiguration.class))
        .withPropertyValues("spring.data.redis.enabled=false")
        .withPropertyValues("auth.token.secret=" + SECRET)
        .run(context -> assertThat(context)
            .hasSingleBean(AuthTokenPort.class)
            .hasSingleBean(AuthTokenLocalAdapter.class));
  }

  @Test
  @DisplayName("Redis 활성화 시 회원가입 인증 토큰에 로컬 캐시를 사용하지 않는다")
  void doesNotUseLocalCacheForSignupAuthTokenWhenRedisEnabled() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AuthTokenLocalAdapterConfiguration.class))
        .withPropertyValues("spring.data.redis.enabled=true")
        .withPropertyValues("auth.token.secret=" + SECRET)
        .run(context -> assertThat(context)
            .doesNotHaveBean(AuthTokenLocalAdapter.class)
            .doesNotHaveBean(AuthTokenPort.class));
  }

  @Test
  @DisplayName("REDIS_ENABLED=false 설정은 회원가입 인증 토큰에 로컬 캐시를 활용한다")
  void redisEnabledFalseUsesLocalCacheForSignupAuthToken() {
    new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withConfiguration(AutoConfigurations.of(AuthTokenLocalAdapterConfiguration.class))
        .withPropertyValues("spring.profiles.active=local")
        .withPropertyValues("REDIS_ENABLED=false")
        .withPropertyValues("auth.token.secret=" + SECRET)
        .run(context -> {
          assertThat(context.getEnvironment()
              .getProperty("spring.data.redis.enabled", Boolean.class))
              .isFalse();
          assertThat(context)
              .hasSingleBean(AuthTokenPort.class)
              .hasSingleBean(AuthTokenLocalAdapter.class);

          AuthTokenPort authTokenPort = context.getBean(AuthTokenPort.class);
          authTokenPort.save(token("raw-token", 0, 3,
              Instant.now().plus(Duration.ofMinutes(5)))).block();

          assertThat(authTokenPort
              .consume(AuthTokenType.EMAIL_VERIFICATION, SUBJECT, "raw-token")
              .block())
              .isEqualTo(AuthTokenConsumeResult.CONSUMED);
          assertThat(authTokenPort
              .consume(AuthTokenType.EMAIL_VERIFICATION, SUBJECT, "raw-token")
              .block())
              .isEqualTo(AuthTokenConsumeResult.MISSING);
        });
  }

  @Test
  @DisplayName("Redis 비활성화 시 AuthTokenPort로 로컬 캐시를 등록한다")
  void redisDisabledRegistersLocalCacheAsAuthTokenPort() {
    new ApplicationContextRunner()
        .withUserConfiguration(AuthTokenLocalAdapter.class)
        .withPropertyValues("spring.data.redis.enabled=false")
        .withPropertyValues("auth.token.secret=" + SECRET)
        .run(context -> assertThat(context)
            .hasSingleBean(AuthTokenPort.class)
            .hasSingleBean(AuthTokenLocalAdapter.class));
  }

  @Test
  @DisplayName("저장된 토큰의 만료 시간을 조회한다")
  void findExpiresAt_returnsStoredTokenExpiry() {
    AuthTokenLocalAdapter sut = new AuthTokenLocalAdapter(SECRET);
    Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));

    sut.save(token("raw-token", 0, 3, expiresAt)).block();

    assertThat(sut.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, SUBJECT).block())
        .isEqualTo(expiresAt);
  }

  @Test
  @DisplayName("만료된 토큰은 조회 시 제거한다")
  void findExpiresAt_expiredTokenRemovesToken() {
    AuthTokenLocalAdapter sut = new AuthTokenLocalAdapter(SECRET);

    sut.save(token("raw-token", 0, 3, Instant.now().minusSeconds(1))).block();

    assertThat(sut.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, SUBJECT).block())
        .isNull();
    assertThat(sut.consume(AuthTokenType.EMAIL_VERIFICATION, SUBJECT, "raw-token").block())
        .isEqualTo(AuthTokenConsumeResult.MISSING);
  }

  @Test
  @DisplayName("유효한 토큰이 있으면 saveIfAbsent가 overwrite하지 않는다")
  void saveIfAbsent_keepsExistingToken() {
    AuthTokenLocalAdapter sut = new AuthTokenLocalAdapter(SECRET);
    Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));

    Boolean firstSaved = sut.saveIfAbsent(token("first-token", 0, 3, expiresAt)).block();
    Boolean secondSaved = sut.saveIfAbsent(token("second-token", 0, 3, expiresAt)).block();

    assertThat(firstSaved).isTrue();
    assertThat(secondSaved).isFalse();
    assertThat(sut.consume(AuthTokenType.EMAIL_VERIFICATION, SUBJECT, "second-token").block())
        .isEqualTo(AuthTokenConsumeResult.MISMATCH);
    assertThat(sut.consume(AuthTokenType.EMAIL_VERIFICATION, SUBJECT, "first-token").block())
        .isEqualTo(AuthTokenConsumeResult.CONSUMED);
  }

  @Test
  @DisplayName("올바른 토큰 consume은 토큰을 삭제하고 재사용을 막는다")
  void consume_matchingTokenConsumesAndDeletesToken() {
    AuthTokenLocalAdapter sut = new AuthTokenLocalAdapter(SECRET);

    sut.save(token("raw-token", 0, 3, Instant.now().plus(Duration.ofMinutes(5)))).block();

    assertThat(sut.consume(AuthTokenType.EMAIL_VERIFICATION, SUBJECT, "raw-token").block())
        .isEqualTo(AuthTokenConsumeResult.CONSUMED);
    assertThat(sut.consume(AuthTokenType.EMAIL_VERIFICATION, SUBJECT, "raw-token").block())
        .isEqualTo(AuthTokenConsumeResult.MISSING);
  }

  @Test
  @DisplayName("잘못된 토큰 consume은 시도 횟수를 증가시키고 한도 초과 시 삭제한다")
  void consume_mismatchIncrementsAttemptsAndDeletesWhenExceeded() {
    AuthTokenLocalAdapter sut = new AuthTokenLocalAdapter(SECRET);

    sut.save(token("raw-token", 0, 2, Instant.now().plus(Duration.ofMinutes(5)))).block();

    assertThat(sut.consume(AuthTokenType.EMAIL_VERIFICATION, SUBJECT, "wrong-token").block())
        .isEqualTo(AuthTokenConsumeResult.MISMATCH);
    assertThat(sut.consume(AuthTokenType.EMAIL_VERIFICATION, SUBJECT, "wrong-token").block())
        .isEqualTo(AuthTokenConsumeResult.ATTEMPTS_EXCEEDED);
    assertThat(sut.consume(AuthTokenType.EMAIL_VERIFICATION, SUBJECT, "raw-token").block())
        .isEqualTo(AuthTokenConsumeResult.MISSING);
  }

  @Test
  @DisplayName("만료된 토큰은 저장하지 않는다")
  void save_expiredTokenDoesNotStore() {
    AuthTokenLocalAdapter sut = new AuthTokenLocalAdapter(SECRET);

    sut.save(token("raw-token", 0, 3, Instant.now().minusSeconds(1))).block();

    assertThat(sut.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, SUBJECT).block())
        .isNull();
  }

  private AuthToken token(String rawToken, int attemptCount,
      int maxAttemptCount, Instant expiresAt) {
    return new AuthToken(
        AuthTokenType.EMAIL_VERIFICATION,
        SUBJECT,
        rawToken,
        attemptCount,
        maxAttemptCount,
        expiresAt);
  }

  @Import(AuthTokenLocalAdapter.class)
  private static class AuthTokenLocalAdapterConfiguration {
  }

}
