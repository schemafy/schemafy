package com.schemafy.api.erd.service.sync;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErdStateSnapshotProducer")
class ErdStateSnapshotProducerTest {

  @Mock
  private ErdStateSnapshotJobStore jobStore;

  private ErdStateSnapshotProducer producer;

  @BeforeEach
  void setUp() {
    producer = new ErdStateSnapshotProducer(jobStore);
  }

  @Test
  @DisplayName("ACTIVE enqueue는 subscription 전 Redis store를 호출하지 않는다")
  void activeEnqueueIsDeferredUntilSubscription() {
    AtomicBoolean storeSubscribed = new AtomicBoolean();
    given(jobStore.enqueueActive("project-1", "schema-1", 42L))
        .willReturn(Mono.defer(() -> {
          storeSubscribed.set(true);
          return Mono.empty();
        }));

    Mono<Void> enqueue = producer.enqueueActive("project-1", "schema-1", 42L);

    assertThat(storeSubscribed).isFalse();
    StepVerifier.create(enqueue).verifyComplete();
    assertThat(storeSubscribed).isTrue();
  }

  @Test
  @DisplayName("DELETED enqueue는 Redis store 실패를 subscriber에게 전달한다")
  void deletedEnqueuePropagatesStoreFailure() {
    IllegalStateException failure = new IllegalStateException("Redis down");
    given(jobStore.enqueueDeleted("project-1", "schema-1", 43L))
        .willReturn(Mono.error(failure));

    StepVerifier.create(
        producer.enqueueDeleted("project-1", "schema-1", 43L))
        .expectErrorSatisfies(error -> assertThat(error).isSameAs(failure))
        .verify();
  }

}
