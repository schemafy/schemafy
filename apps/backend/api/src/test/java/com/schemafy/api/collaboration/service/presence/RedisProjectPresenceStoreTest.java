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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisProjectPresenceStore 단위 테스트")
@SuppressWarnings({ "unchecked", "rawtypes" })
class RedisProjectPresenceStoreTest {

  private static final String PARTICIPANTS_KEY = "collaboration:presence:project:project-1:participants";
  private static final String EXPIRES_KEY = "collaboration:presence:project:project-1:expires";
  private static final String ACTIVE_PROJECTS_KEY = "collaboration:presence:projects";

  @Mock
  private ReactiveStringRedisTemplate redisTemplate;

  @Mock
  private ReactiveZSetOperations<String, String> zSetOps;

  @Mock
  private ReactiveHashOperations<String, String, String> hashOps;

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
  @DisplayName("만료 후보가 heartbeat로 갱신되면 결과 없이 빈 프로젝트 cleanup만 확인한다")
  void removeExpired_ignores_session_refreshed_after_candidate_scan() {
    given(redisTemplate.opsForZSet()).willReturn(zSetOps);
    given(zSetOps.rangeByScore(eq(EXPIRES_KEY), any(Range.class)))
        .willReturn(Flux.just("session-1"));
    given(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
        .willReturn(Flux.empty(), Flux.just(0L));

    StepVerifier.create(presenceStore.removeExpired("project-1"))
        .verifyComplete();

    ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(
        List.class);
    ArgumentCaptor<List> argsCaptor = ArgumentCaptor.forClass(List.class);
    verify(redisTemplate, times(2)).execute(any(RedisScript.class),
        keysCaptor.capture(), argsCaptor.capture());

    assertThat(keysCaptor.getAllValues().get(0)).containsExactly(PARTICIPANTS_KEY,
        EXPIRES_KEY);
    assertThat(argsCaptor.getAllValues().get(0)).first().isEqualTo("session-1");
    assertThat(keysCaptor.getAllValues().get(1)).containsExactly(
        ACTIVE_PROJECTS_KEY, PARTICIPANTS_KEY, EXPIRES_KEY);
    assertThat(argsCaptor.getAllValues().get(1)).first().isEqualTo("project-1");

    verify(zSetOps, never()).remove(EXPIRES_KEY, "session-1");
  }

  @Test
  @DisplayName("만료 payload 역직렬화가 실패해도 빈 프로젝트 cleanup을 확인한다")
  void removeExpired_checks_empty_project_when_expired_payload_is_invalid() {
    given(redisTemplate.opsForZSet()).willReturn(zSetOps);
    given(zSetOps.rangeByScore(eq(EXPIRES_KEY), any(Range.class)))
        .willReturn(Flux.just("session-1"));
    given(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
        .willReturn(Flux.just("{"), Flux.just(0L));

    StepVerifier.create(presenceStore.removeExpired("project-1"))
        .verifyComplete();

    ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(
        List.class);
    verify(redisTemplate, times(2)).execute(any(RedisScript.class),
        keysCaptor.capture(), anyList());

    assertThat(keysCaptor.getAllValues().get(0)).containsExactly(PARTICIPANTS_KEY,
        EXPIRES_KEY);
    assertThat(keysCaptor.getAllValues().get(1)).containsExactly(
        ACTIVE_PROJECTS_KEY, PARTICIPANTS_KEY, EXPIRES_KEY);
  }

  @Test
  @DisplayName("만료 세션은 현재 score 조건을 확인하는 Redis script로 삭제한다")
  void removeExpired_deletes_session_with_guarded_script() {
    ProjectPresenceSession expired = new ProjectPresenceSession(
        "session-1", "user-1", "tester", 1000L, 1000L);
    String payload = jsonCodec.toJson(expired);

    given(redisTemplate.opsForZSet()).willReturn(zSetOps);
    given(zSetOps.rangeByScore(eq(EXPIRES_KEY), any(Range.class)))
        .willReturn(Flux.just("session-1"));
    given(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
        .willReturn(Flux.just(payload), Flux.just(0L));

    StepVerifier.create(presenceStore.removeExpired("project-1"))
        .assertNext(session -> assertThat(session.sessionId())
            .isEqualTo("session-1"))
        .verifyComplete();

    ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(
        List.class);
    ArgumentCaptor<List> argsCaptor = ArgumentCaptor.forClass(List.class);
    verify(redisTemplate, times(2)).execute(any(RedisScript.class),
        keysCaptor.capture(), argsCaptor.capture());

    assertThat(keysCaptor.getAllValues().get(0)).containsExactly(PARTICIPANTS_KEY,
        EXPIRES_KEY);
    assertThat(argsCaptor.getAllValues().get(0)).first().isEqualTo("session-1");
    assertThat(keysCaptor.getAllValues().get(1)).containsExactly(
        ACTIVE_PROJECTS_KEY, PARTICIPANTS_KEY, EXPIRES_KEY);
    assertThat(argsCaptor.getAllValues().get(1)).first().isEqualTo("project-1");

    verify(hashOps, never()).size(PARTICIPANTS_KEY);
    verify(redisTemplate, never()).delete(PARTICIPANTS_KEY);
    verify(redisTemplate, never()).delete(EXPIRES_KEY);
  }

  @Test
  @DisplayName("presence refresh는 Redis script 한 번으로 payload와 만료 score를 갱신한다")
  void refresh_updates_presence_metadata_atomically() {
    ProjectPresenceSession refreshed = new ProjectPresenceSession(
        "session-1", "user-1", "tester", 1000L, 2000L);
    String payload = jsonCodec.toJson(refreshed);

    given(redisTemplate.execute(eq(ProjectPresenceRedisScripts.REFRESH_SESSION),
        anyList(), anyList()))
        .willReturn(Flux.just(payload));

    StepVerifier.create(presenceStore.refresh("project-1", "session-1"))
        .assertNext(session -> {
          assertThat(session.sessionId()).isEqualTo("session-1");
          assertThat(session.lastSeenAt()).isEqualTo(2000L);
        })
        .verifyComplete();

    ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(
        List.class);
    ArgumentCaptor<List> argsCaptor = ArgumentCaptor.forClass(List.class);
    verify(redisTemplate).execute(eq(ProjectPresenceRedisScripts.REFRESH_SESSION),
        keysCaptor.capture(), argsCaptor.capture());

    assertThat(keysCaptor.getValue()).containsExactly(PARTICIPANTS_KEY,
        EXPIRES_KEY, ACTIVE_PROJECTS_KEY);
    assertThat(argsCaptor.getValue()).hasSize(4);
    assertThat(argsCaptor.getValue().get(0)).isEqualTo("session-1");
    long lastSeenAt = Long.parseLong((String) argsCaptor.getValue().get(1));
    double expiresAt = Double.parseDouble((String) argsCaptor.getValue().get(2));
    assertThat(expiresAt).isEqualTo(lastSeenAt + Duration.ofSeconds(90)
        .toMillis());
    assertThat(argsCaptor.getValue().get(3)).isEqualTo("project-1");

    verify(redisTemplate, never()).opsForHash();
    verify(redisTemplate, never()).opsForZSet();
  }

  @Test
  @DisplayName("presence refresh가 세션을 찾지 못하면 재등록 경로를 위해 empty로 끝낸다")
  void refresh_returns_empty_when_session_is_missing() {
    given(redisTemplate.execute(eq(ProjectPresenceRedisScripts.REFRESH_SESSION),
        anyList(), anyList()))
        .willReturn(Flux.empty());

    StepVerifier.create(presenceStore.refresh("project-1", "session-1"))
        .verifyComplete();

    verify(redisTemplate).execute(eq(ProjectPresenceRedisScripts.REFRESH_SESSION),
        anyList(), anyList());
    verify(redisTemplate, never()).opsForHash();
  }

  @Test
  @DisplayName("presence 등록은 참가자, 만료 score, active project marker를 원자적으로 기록한다")
  void register_writes_presence_metadata_atomically() {
    given(redisTemplate.<String, String>opsForHash()).willReturn(hashOps);
    given(hashOps.get(PARTICIPANTS_KEY, "session-1"))
        .willReturn(Mono.empty());
    given(redisTemplate.execute(any(RedisScript.class), anyList(), anyList()))
        .willReturn(Flux.just(1L));

    StepVerifier.create(presenceStore.register("project-1", "session-1",
        "user-1", "tester"))
        .assertNext(session -> assertThat(session.sessionId())
            .isEqualTo("session-1"))
        .verifyComplete();

    ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(
        List.class);
    ArgumentCaptor<List> argsCaptor = ArgumentCaptor.forClass(List.class);
    verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(),
        argsCaptor.capture());

    assertThat(keysCaptor.getValue()).containsExactly(PARTICIPANTS_KEY,
        EXPIRES_KEY, ACTIVE_PROJECTS_KEY);
    assertThat(argsCaptor.getValue()).hasSize(4);
    assertThat(argsCaptor.getValue().get(0)).isEqualTo("session-1");
    assertThat(argsCaptor.getValue().get(1)).asString()
        .contains("\"sessionId\":\"session-1\"");
    assertThat(argsCaptor.getValue().get(2)).asString()
        .isNotBlank();
    assertThat(argsCaptor.getValue().get(3)).isEqualTo("project-1");

    verify(hashOps, never()).put(eq(PARTICIPANTS_KEY), eq("session-1"), any());
    verify(redisTemplate, never()).opsForZSet();
    verify(redisTemplate, never()).opsForSet();
  }

  @Test
  @DisplayName("presence Redis script 리소스를 classpath에서 로드한다")
  void redisScripts_are_loaded_from_classpath_resources() {
    assertThat(ProjectPresenceRedisScripts.WRITE_SESSION.getScriptAsString())
        .contains("HSET", "ZADD", "SADD");
    assertThat(ProjectPresenceRedisScripts.REFRESH_SESSION.getScriptAsString())
        .contains("HGET", "HSET", "ZADD", "cjson.decode");
    assertThat(ProjectPresenceRedisScripts.REMOVE_EXPIRED_SESSION
        .getScriptAsString())
        .contains("ZSCORE", "HDEL", "ZREM");
    assertThat(ProjectPresenceRedisScripts.CLEANUP_EMPTY_PROJECT
        .getScriptAsString())
        .contains("HLEN", "SREM", "DEL");
  }

}
