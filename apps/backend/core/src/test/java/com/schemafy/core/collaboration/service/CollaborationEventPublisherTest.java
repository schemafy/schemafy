package com.schemafy.core.collaboration.service;

import java.util.Set;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemafy.core.collaboration.dto.event.ErdMutatedEvent;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollaborationEventPublisher 단위 테스트")
class CollaborationEventPublisherTest {

  private static final String CHANNEL = "collaboration:project-1";

  @Mock
  private ReactiveStringRedisTemplate redisTemplate;

  private ObjectMapper objectMapper;
  private CollaborationEventPublisher publisher;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    publisher = new CollaborationEventPublisher(redisTemplate,
        new JsonCodec(objectMapper));
  }

  @Test
  @DisplayName("프로젝트 채널에 ERD_MUTATED JSON payload를 발행한다")
  void publishes_erd_mutated_payload_to_project_channel() throws Exception {
    ErdMutatedEvent.Outbound event = new ErdMutatedEvent.Outbound(
        null,
        "schema-1",
        Set.of("table-1"),
        new CommittedErdOperation(
            "op-1",
            "client-op-1",
            42L,
            ErdOperationDerivationKind.ORIGINAL),
        123L);
    given(redisTemplate.convertAndSend(eq(CHANNEL), anyString()))
        .willReturn(Mono.just(1L));

    StepVerifier.create(publisher.publish("project-1", event))
        .verifyComplete();

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(
        String.class);
    verify(redisTemplate).convertAndSend(eq(CHANNEL),
        payloadCaptor.capture());

    JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
    assertThat(payload.path("type").asText()).isEqualTo("ERD_MUTATED");
    assertThat(payload.has("sessionId")).isFalse();
    assertThat(payload.path("schemaId").asText()).isEqualTo("schema-1");
    assertThat(payload.path("affectedTableIds").get(0).asText())
        .isEqualTo("table-1");
    assertThat(payload.path("operation").path("opId").asText())
        .isEqualTo("op-1");
    assertThat(payload.path("operation").path("clientOperationId").asText())
        .isEqualTo("client-op-1");
    assertThat(payload.path("operation").path("committedRevision").asLong())
        .isEqualTo(42L);
    assertThat(payload.path("operation").path("derivationKind").asText())
        .isEqualTo("ORIGINAL");
    assertThat(payload.path("timestamp").asLong()).isEqualTo(123L);
  }

  @Test
  @DisplayName("Redis 발행 오류는 호출자에게 전파하지 않는다")
  void swallows_redis_publish_error() {
    ErdMutatedEvent.Outbound event = ErdMutatedEvent.Outbound.of(
        null,
        "schema-1",
        Set.of("table-1"),
        new CommittedErdOperation(
            "op-1",
            "client-op-1",
            42L,
            ErdOperationDerivationKind.ORIGINAL));
    given(redisTemplate.convertAndSend(eq(CHANNEL), anyString()))
        .willReturn(Mono.error(new RuntimeException("Redis down")));

    StepVerifier.create(publisher.publish("project-1", event))
        .verifyComplete();
  }

  @Test
  @DisplayName("strict 발행은 Redis 오류를 호출자에게 전파한다")
  void strict_publish_propagates_redis_error() {
    ErdMutatedEvent.Outbound event = ErdMutatedEvent.Outbound.of(
        null,
        "schema-1",
        Set.of("table-1"),
        new CommittedErdOperation(
            "op-1",
            "client-op-1",
            42L,
            ErdOperationDerivationKind.ORIGINAL));
    given(redisTemplate.convertAndSend(eq(CHANNEL), anyString()))
        .willReturn(Mono.error(new RuntimeException("Redis down")));

    StepVerifier.create(publisher.publishStrict("project-1", event))
        .expectErrorMatches(error -> error instanceof RuntimeException
            && error.getMessage().equals("Redis down"))
        .verify();
  }

}
