package com.schemafy.api.user.adapter.out;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.user.domain.AuthToken;
import com.schemafy.core.user.domain.AuthTokenConsumeResult;
import com.schemafy.core.user.domain.AuthTokenType;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthTokenRedisAdapter")
@SuppressWarnings({ "unchecked", "rawtypes" })
class AuthTokenRedisAdapterTest {

  private static final String USER_ID = "user-1";
  private static final String KEY = "auth-token:EMAIL_VERIFICATION:" + USER_ID;

  @Mock
  private ReactiveStringRedisTemplate redisTemplate;

  @Mock
  private ReactiveValueOperations<String, String> valueOps;

  private AuthTokenRedisAdapter sut;
  private JsonCodec jsonCodec;

  @BeforeEach
  void setUp() {
    jsonCodec = jsonCodec();
    sut = new AuthTokenRedisAdapter(redisTemplate, jsonCodec,
        "test-auth-token-secret");
  }

  @Test
  @DisplayName("저장된 토큰의 만료 시간을 조회한다")
  void findExpiresAt_returnsStoredTokenExpiry() {
    Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
    given(redisTemplate.opsForValue()).willReturn(valueOps);
    given(valueOps.get(KEY)).willReturn(Mono.just(payload("token-hash", 0, 3,
        expiresAt)));
    given(redisTemplate.getExpire(KEY)).willReturn(Mono.just(Duration.ofMinutes(5)));

    Instant found = sut.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, USER_ID)
        .block();

    assertThat(found).isEqualTo(expiresAt);
  }

  @Test
  @DisplayName("없는 토큰 조회는 empty를 반환한다")
  void findExpiresAt_missingTokenReturnsEmpty() {
    given(redisTemplate.opsForValue()).willReturn(valueOps);
    given(valueOps.get(KEY)).willReturn(Mono.empty());

    Instant found = sut.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, USER_ID)
        .block();

    assertThat(found).isNull();
  }

  @Test
  @DisplayName("TTL이 없는 토큰 조회는 손상된 토큰으로 보고 삭제한다")
  void findExpiresAt_tokenWithoutTtlDeletesKey() {
    given(redisTemplate.opsForValue()).willReturn(valueOps);
    given(valueOps.get(KEY)).willReturn(Mono.just(payload("token-hash", 0, 3,
        Instant.now().plus(Duration.ofMinutes(5)))));
    given(redisTemplate.getExpire(KEY)).willReturn(Mono.just(Duration.ZERO));
    given(redisTemplate.delete(KEY)).willReturn(Mono.just(1L));

    Instant found = sut.findExpiresAt(AuthTokenType.EMAIL_VERIFICATION, USER_ID)
        .block();

    assertThat(found).isNull();
    verify(redisTemplate).delete(KEY);
  }

  @Test
  @DisplayName("토큰을 TTL과 함께 해시 payload로 저장한다")
  void save_storesHashedPayloadWithTtl() {
    Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
    given(redisTemplate.opsForValue()).willReturn(valueOps);
    given(valueOps.set(eq(KEY), anyString(), any(Duration.class)))
        .willReturn(Mono.just(true));

    sut.save(token("raw-token", 0, 3, expiresAt)).block();

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
    verify(valueOps).set(eq(KEY), payloadCaptor.capture(), ttlCaptor.capture());
    assertThat(payloadCaptor.getValue())
        .contains("\"tokenHash\"")
        .contains("\"attemptCount\":0")
        .contains("\"maxAttemptCount\":3")
        .doesNotContain("raw-token");
    assertThat(ttlCaptor.getValue()).isPositive();
  }

  @Test
  @DisplayName("만료된 토큰은 저장하지 않는다")
  void save_expiredTokenDoesNotWrite() {
    sut.save(token("raw-token", 0, 3, Instant.now().minusSeconds(1))).block();

    verify(redisTemplate, never()).opsForValue();
  }

  @Test
  @DisplayName("유효한 토큰이 있으면 saveIfAbsent가 overwrite하지 않는다")
  void saveIfAbsent_keepsExistingToken() {
    given(redisTemplate.opsForValue()).willReturn(valueOps);
    given(valueOps.setIfAbsent(eq(KEY), anyString(), any(Duration.class)))
        .willReturn(Mono.just(false));

    Boolean saved = sut.saveIfAbsent(
        token("new-token", 0, 3, Instant.now().plus(Duration.ofMinutes(5))))
        .block();

    assertThat(saved).isFalse();
  }

  @Test
  @DisplayName("토큰이 없으면 saveIfAbsent가 새 토큰을 해시로 저장한다")
  void saveIfAbsent_savesWhenMissing() {
    given(redisTemplate.opsForValue()).willReturn(valueOps);
    given(valueOps.setIfAbsent(eq(KEY), anyString(), any(Duration.class)))
        .willReturn(Mono.just(true));

    Boolean saved = sut.saveIfAbsent(
        token("raw-token", 0, 3, Instant.now().plus(Duration.ofMinutes(5))))
        .block();

    assertThat(saved).isTrue();
    verify(valueOps).setIfAbsent(eq(KEY), anyString(), any(Duration.class));
  }

  @Test
  @DisplayName("토큰을 삭제한다")
  void delete_removesToken() {
    given(redisTemplate.delete(KEY)).willReturn(Mono.just(1L));

    sut.delete(AuthTokenType.EMAIL_VERIFICATION, USER_ID).block();

    verify(redisTemplate).delete(KEY);
  }

  @Test
  @DisplayName("Lua script 결과를 consume 결과로 변환한다")
  void consume_mapsScriptResult() {
    given(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
        .willReturn(Flux.just("CONSUMED"));

    AuthTokenConsumeResult result = sut.consume(
        AuthTokenType.EMAIL_VERIFICATION, USER_ID, "raw-token")
        .block();

    assertThat(result).isEqualTo(AuthTokenConsumeResult.CONSUMED);
  }

  @Test
  @DisplayName("Lua script 결과가 없으면 missing으로 처리한다")
  void consume_emptyScriptResultReturnsMissing() {
    given(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
        .willReturn(Flux.empty());

    AuthTokenConsumeResult result = sut.consume(
        AuthTokenType.EMAIL_VERIFICATION, USER_ID, "raw-token")
        .block();

    assertThat(result).isEqualTo(AuthTokenConsumeResult.MISSING);
  }

  @Test
  @DisplayName("consume은 raw token 대신 해시된 토큰을 Redis script에 전달한다")
  void consume_passesHashedTokenToScript() {
    given(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
        .willReturn(Flux.just("MISMATCH"));

    sut.consume(AuthTokenType.EMAIL_VERIFICATION, USER_ID, "raw-token").block();

    ArgumentCaptor<List> keysCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<List> argsCaptor = ArgumentCaptor.forClass(List.class);
    verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(),
        argsCaptor.capture());

    assertThat(keysCaptor.getValue()).containsExactly(KEY);
    assertThat(argsCaptor.getValue()).hasSize(1);
    assertThat(argsCaptor.getValue().get(0)).isNotEqualTo("raw-token");
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

  private String payload(String tokenHash, int attemptCount,
      int maxAttemptCount, Instant expiresAt) {
    return """
        {"tokenHash":"%s","attemptCount":%d,"maxAttemptCount":%d,"expiresAt":"%s"}
        """.formatted(tokenHash, attemptCount, maxAttemptCount, expiresAt);
  }

  private JsonCodec jsonCodec() {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return new JsonCodec(objectMapper);
  }

}
