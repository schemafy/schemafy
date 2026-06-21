package com.schemafy.api.user.adapter.out;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("AuthTokenRedisAdapter")
class AuthTokenRedisAdapterTest {

  private static final String USER_ID = "user-1";
  private static final String KEY = "auth-token:EMAIL_VERIFICATION:" + USER_ID;

  @Container
  static final GenericContainer<?> redis = new GenericContainer<>("redis:8.4-alpine")
      .withExposedPorts(6379);

  private LettuceConnectionFactory connectionFactory;
  private ReactiveStringRedisTemplate redisTemplate;
  private AuthTokenRedisAdapter sut;

  @BeforeEach
  void setUp() {
    connectionFactory = new LettuceConnectionFactory(redis.getHost(),
        redis.getMappedPort(6379));
    connectionFactory.afterPropertiesSet();
    redisTemplate = new ReactiveStringRedisTemplate(connectionFactory);
    sut = new AuthTokenRedisAdapter(redisTemplate, jsonCodec(), "test-auth-token-secret");

    redisTemplate.execute(connection -> connection.serverCommands().flushDb())
        .then()
        .block();
  }

  @AfterEach
  void tearDown() {
    if (connectionFactory != null) {
      connectionFactory.destroy();
    }
  }

  @Test
  @DisplayName("저장된 토큰의 만료 시간을 조회한다")
  void findExpiresAt_returnsStoredTokenExpiry() {
    Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
    sut.save(token("raw-token", 1, 5, expiresAt)).block();

    Instant found = sut.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, USER_ID)
        .block();

    assertThat(found).isEqualTo(expiresAt);
  }

  @Test
  @DisplayName("없는 토큰 조회는 empty를 반환한다")
  void findExpiresAt_missingTokenReturnsEmpty() {
    Instant found = sut.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, USER_ID)
        .block();

    assertThat(found).isNull();
  }

  @Test
  @DisplayName("TTL이 없는 토큰 조회는 손상된 토큰으로 보고 삭제한다")
  void findExpiresAt_tokenWithoutTtlDeletesKey() {
    Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
    redisTemplate.opsForValue()
        .set(KEY, "{\"tokenHash\":\"token-hash\",\"attemptCount\":0,\"maxAttemptCount\":5,\"expiresAt\":\""
            + expiresAt + "\"}")
        .block();

    Instant found = sut.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, USER_ID)
        .block();
    Boolean existsAfterFind = redisTemplate.hasKey(KEY).block();

    assertThat(found).isNull();
    assertThat(existsAfterFind).isFalse();
  }

  @Test
  @DisplayName("유효한 토큰이 있으면 saveIfAbsent가 overwrite하지 않는다")
  void saveIfAbsent_keepsExistingToken() {
    sut.save(token("old-token", 0, 5, Instant.now().plus(Duration.ofMinutes(5))))
        .block();

    Boolean saved = sut.saveIfAbsent(
        token("new-token", 0, 5, Instant.now().plus(Duration.ofMinutes(5))))
        .block();
    String payload = redisTemplate.opsForValue().get(KEY).block();

    assertThat(saved).isFalse();
    assertThat(payload).doesNotContain("old-token", "new-token");
  }

  @Test
  @DisplayName("토큰이 없으면 saveIfAbsent가 새 토큰을 해시로 저장한다")
  void saveIfAbsent_savesWhenMissing() {
    Boolean saved = sut.saveIfAbsent(
        token("raw-token", 0, 5, Instant.now().plus(Duration.ofMinutes(5))))
        .block();
    String payload = redisTemplate.opsForValue().get(KEY).block();

    assertThat(saved).isTrue();
    assertThat(payload).contains("tokenHash").doesNotContain("raw-token");
  }

  @Test
  @DisplayName("토큰을 TTL과 함께 저장하고 같은 key 재발급 시 overwrite한다")
  void save_setsTtlAndOverwritesExistingToken() {
    sut.save(token("old-token", 0, 5, Instant.now().plus(Duration.ofMinutes(5))))
        .block();

    Long ttlMillis = redisTemplate.getExpire(KEY)
        .map(Duration::toMillis)
        .block();
    String firstPayload = redisTemplate.opsForValue().get(KEY).block();

    sut.save(token("new-token", 0, 5, Instant.now().plus(Duration.ofMinutes(5))))
        .block();

    String overwrittenPayload = redisTemplate.opsForValue().get(KEY).block();
    AuthTokenConsumeResult oldCodeResult = sut.consume(
        AuthTokenType.EMAIL_VERIFICATION, USER_ID, "old-token")
        .block();
    AuthTokenConsumeResult newCodeResult = sut.consume(
        AuthTokenType.EMAIL_VERIFICATION, USER_ID, "new-token")
        .block();

    assertThat(ttlMillis).isNotNull().isPositive().isLessThanOrEqualTo(300_000);
    assertThat(firstPayload).doesNotContain("old-token");
    assertThat(overwrittenPayload).doesNotContain("new-token", "old-token");
    assertThat(oldCodeResult).isEqualTo(AuthTokenConsumeResult.MISMATCH);
    assertThat(newCodeResult).isEqualTo(AuthTokenConsumeResult.CONSUMED);
  }

  @Test
  @DisplayName("토큰을 삭제한다")
  void delete_removesToken() {
    sut.save(token("raw-token", 0, 5, Instant.now().plus(Duration.ofMinutes(5))))
        .block();

    sut.delete(AuthTokenType.EMAIL_VERIFICATION, USER_ID).block();
    Boolean exists = redisTemplate.hasKey(KEY).block();

    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("코드가 맞으면 Lua script가 원자적으로 consume/delete한다")
  void consume_matchingTokenDeletesKeyAndPreventsReuse() {
    sut.save(token("raw-token", 0, 5, Instant.now().plus(Duration.ofMinutes(5))))
        .block();

    AuthTokenConsumeResult first = sut.consume(
        AuthTokenType.EMAIL_VERIFICATION, USER_ID, "raw-token")
        .block();
    AuthTokenConsumeResult second = sut.consume(
        AuthTokenType.EMAIL_VERIFICATION, USER_ID, "raw-token")
        .block();
    Boolean existsAfterConsume = redisTemplate.hasKey(KEY).block();

    assertThat(first).isEqualTo(AuthTokenConsumeResult.CONSUMED);
    assertThat(second).isEqualTo(AuthTokenConsumeResult.MISSING);
    assertThat(existsAfterConsume).isFalse();
  }

  @Test
  @DisplayName("코드가 틀리면 attemptCount를 증가시키고 최대 횟수에서 삭제한다")
  void consume_mismatchIncrementsAttemptsAndDeletesAtMaxAttempts() {
    sut.save(token("raw-token", 0, 2, Instant.now().plus(Duration.ofMinutes(5))))
        .block();

    AuthTokenConsumeResult firstMismatch = sut.consume(
        AuthTokenType.EMAIL_VERIFICATION, USER_ID, "wrong-hash-1")
        .block();
    String payloadAfterFirstMismatch = redisTemplate.opsForValue().get(KEY).block();

    AuthTokenConsumeResult secondMismatch = sut.consume(
        AuthTokenType.EMAIL_VERIFICATION, USER_ID, "wrong-hash-2")
        .block();
    Boolean existsAfterMaxAttempts = redisTemplate.hasKey(KEY).block();

    assertThat(firstMismatch).isEqualTo(AuthTokenConsumeResult.MISMATCH);
    assertThat(payloadAfterFirstMismatch).contains("\"attemptCount\":1");
    assertThat(secondMismatch).isEqualTo(AuthTokenConsumeResult.ATTEMPTS_EXCEEDED);
    assertThat(existsAfterMaxAttempts).isFalse();
  }

  @Test
  @DisplayName("payload에 시도 횟수 정책이 없으면 손상된 토큰으로 보고 삭제한다")
  void consume_missingAttemptPolicyDeletesKey() {
    redisTemplate.opsForValue()
        .set(KEY, "{\"tokenHash\":\"token-hash\",\"attemptCount\":0}")
        .block();

    AuthTokenConsumeResult result = sut.consume(
        AuthTokenType.EMAIL_VERIFICATION, USER_ID, "wrong-hash")
        .block();
    Boolean existsAfterConsume = redisTemplate.hasKey(KEY).block();

    assertThat(result).isEqualTo(AuthTokenConsumeResult.MISSING);
    assertThat(existsAfterConsume).isFalse();
  }

  @Test
  @DisplayName("TTL이 없는 auth token은 손상된 토큰으로 보고 삭제한다")
  void consume_tokenWithoutTtlDeletesKey() {
    redisTemplate.opsForValue()
        .set(KEY, "{\"tokenHash\":\"token-hash\",\"attemptCount\":0,\"maxAttemptCount\":5}")
        .block();

    AuthTokenConsumeResult result = sut.consume(
        AuthTokenType.EMAIL_VERIFICATION, USER_ID, "wrong-hash")
        .block();
    Boolean existsAfterConsume = redisTemplate.hasKey(KEY).block();

    assertThat(result).isEqualTo(AuthTokenConsumeResult.MISSING);
    assertThat(existsAfterConsume).isFalse();
  }

  @Test
  @DisplayName("동시에 같은 코드를 검증해도 하나만 성공한다")
  void consume_concurrentMatchingTokenAllowsSingleSuccess() {
    sut.save(token("raw-token", 0, 10, Instant.now().plus(Duration.ofMinutes(5))))
        .block();

    List<AuthTokenConsumeResult> results = Flux.range(0, 10)
        .flatMap(ignored -> sut.consume(
            AuthTokenType.EMAIL_VERIFICATION, USER_ID, "raw-token"), 10)
        .collectList()
        .block();
    Boolean existsAfterConsume = redisTemplate.hasKey(KEY).block();

    assertThat(results).isNotNull();
    assertThat(results).filteredOn(AuthTokenConsumeResult.CONSUMED::equals)
        .hasSize(1);
    assertThat(results).filteredOn(AuthTokenConsumeResult.MISSING::equals)
        .hasSize(9);
    assertThat(existsAfterConsume).isFalse();
  }

  private AuthToken token(String rawToken, int attemptCount,
      int maxAttemptCount, Instant expiresAt) {
    return new AuthToken(
        AuthTokenType.EMAIL_VERIFICATION,
        USER_ID,
        rawToken,
        attemptCount,
        maxAttemptCount,
        expiresAt);
  }

  private JsonCodec jsonCodec() {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return new JsonCodec(objectMapper);
  }

}
