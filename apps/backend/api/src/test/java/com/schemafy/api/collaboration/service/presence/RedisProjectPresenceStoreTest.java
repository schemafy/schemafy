package com.schemafy.api.collaboration.service.presence;

import java.time.Duration;
import java.util.List;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.json.JsonCodec;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisProjectPresenceStore 단위 테스트")
@SuppressWarnings({ "unchecked", "rawtypes" })
class RedisProjectPresenceStoreTest {

  private static final String PARTICIPANTS_KEY = "collaboration:presence:project:project-1:participants";
  private static final String EXPIRES_KEY = "collaboration:presence:project:project-1:expires";

  @Mock
  private ReactiveStringRedisTemplate redisTemplate;

  @Mock
  private ReactiveZSetOperations<String, String> zSetOps;

  @Mock
  private ReactiveHashOperations<String, Object, Object> hashOps;

  private JsonCodec jsonCodec;
  private RedisProjectPresenceStore presenceStore;

  @BeforeEach
  void setUp() {
    jsonCodec = new JsonCodec(new ObjectMapper().findAndRegisterModules());
    CollaborationPresenceProperties properties = new CollaborationPresenceProperties();
    properties.setSessionTtl(Duration.ofSeconds(90));
    presenceStore = new RedisProjectPresenceStore(redisTemplate, jsonCodec,
        properties);
  }

  @Test
  @DisplayName("만료 후보가 heartbeat로 갱신되면 삭제 결과를 반환하지 않는다")
  void removeExpired_ignores_session_refreshed_after_candidate_scan() {
    given(redisTemplate.opsForZSet()).willReturn(zSetOps);
    given(zSetOps.rangeByScore(eq(EXPIRES_KEY), any(Range.class)))
        .willReturn(Flux.just("session-1"));
    given(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
        .willReturn(Flux.empty());

    StepVerifier.create(presenceStore.removeExpired("project-1"))
        .verifyComplete();

    verify(zSetOps, never()).remove(EXPIRES_KEY, "session-1");
  }

  @Test
  @DisplayName("만료 세션은 현재 score 조건을 확인하는 Redis script로 삭제한다")
  void removeExpired_deletes_session_with_guarded_script() {
    ProjectPresenceSession expired = new ProjectPresenceSession(
        "session-1", "user-1", "tester", 1000L, 1000L);
    String payload = jsonCodec.serialize(expired);

    given(redisTemplate.opsForZSet()).willReturn(zSetOps);
    given(zSetOps.rangeByScore(eq(EXPIRES_KEY), any(Range.class)))
        .willReturn(Flux.just("session-1"));
    given(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
        .willReturn(Flux.just(payload));
    given(redisTemplate.opsForHash()).willReturn(hashOps);
    given(hashOps.size(PARTICIPANTS_KEY)).willReturn(Mono.just(1L));

    StepVerifier.create(presenceStore.removeExpired("project-1"))
        .assertNext(session -> assertThat(session.sessionId())
            .isEqualTo("session-1"))
        .verifyComplete();

    ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(
        List.class);
    ArgumentCaptor<List> argsCaptor = ArgumentCaptor.forClass(List.class);
    verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(),
        argsCaptor.capture());

    assertThat(keysCaptor.getValue()).containsExactly(PARTICIPANTS_KEY,
        EXPIRES_KEY);
    assertThat(argsCaptor.getValue()).first().isEqualTo("session-1");
  }

}
