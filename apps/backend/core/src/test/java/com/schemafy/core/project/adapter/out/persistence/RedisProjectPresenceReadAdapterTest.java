package com.schemafy.core.project.adapter.out.persistence;

import java.time.Duration;

import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.common.json.JsonCodec;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RedisProjectPresenceReadAdapterTest {

  private final ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
  @SuppressWarnings("unchecked")
  private final ReactiveHashOperations<String, String, String> hash = mock(ReactiveHashOperations.class);
  private final RedisProjectPresenceReadAdapter adapter = new RedisProjectPresenceReadAdapter(
      redis, new JsonCodec(new ObjectMapper()));

  @BeforeEach
  void setup() {
    when(redis.<String, String>opsForHash()).thenReturn(hash);
    ReflectionTestUtils.setField(adapter, "sessionTtl", Duration.ofSeconds(90));
  }

  @Test
  void findsActiveSessionUsingProjectScopedKey() {
    when(hash.get("collaboration:presence:project:p:participants", "owner"))
        .thenReturn(Mono.just(payload("owner", System.currentTimeMillis())));
    StepVerifier.create(adapter.findSession("p", "owner"))
        .assertNext(session -> {
          assertThat(session.sessionId()).isEqualTo("owner");
          assertThat(session.userId()).isEqualTo("user");
        }).verifyComplete();
    verify(hash).get("collaboration:presence:project:p:participants", "owner");
  }

  @Test
  void rejectsMissingExpiredMismatchedAndMalformedSessions() {
    when(hash.get("collaboration:presence:project:p:participants", "owner"))
        .thenReturn(Mono.empty(), Mono.just(payload("owner", 0)),
            Mono.just(payload("other", System.currentTimeMillis())), Mono.just("invalid"));
    for (int attempt = 0; attempt < 4; attempt++) {
      StepVerifier.create(adapter.findSession("p", "owner")).verifyComplete();
    }
  }

  private String payload(String sessionId, long lastSeenAt) {
    return """
        {"sessionId":"%s","userId":"user","userName":"name","joinedAt":0,"lastSeenAt":%d}
        """.formatted(sessionId, lastSeenAt);
  }

}
