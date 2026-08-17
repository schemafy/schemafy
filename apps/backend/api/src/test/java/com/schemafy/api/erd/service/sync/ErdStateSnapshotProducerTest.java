package com.schemafy.api.erd.service.sync;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErdStateSnapshotProducer")
class ErdStateSnapshotProducerTest {

  @Mock
  private ErdStateSnapshotJobStore jobStore;

  private final ErdStateSnapshotProperties properties = properties();
  private final Scheduler scheduler = Schedulers.newSingle("snapshot-producer-test");

  private ErdStateSnapshotProducer producer;

  @BeforeEach
  void setUp() {
    producer = new ErdStateSnapshotProducer(jobStore, properties, scheduler);
  }

  @AfterEach
  void tearDown() {
    scheduler.dispose();
  }

  private static ErdStateSnapshotProperties properties() {
    ErdStateSnapshotProperties properties = new ErdStateSnapshotProperties();
    properties.setRetryBackoff(Duration.ofMillis(1));
    properties.setMaxRetryBackoff(Duration.ofMillis(4));
    return properties;
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
  @DisplayName("ACTIVE enqueue는 일시적 실패 후 재시도로 성공한다")
  void activeEnqueueRecoversAfterTransientFailure() {
    AtomicInteger attempts = new AtomicInteger();
    given(jobStore.enqueueActive("project-1", "schema-1", 42L))
        .willAnswer(invocation -> attempts.getAndIncrement() == 0
            ? Mono.error(new IllegalStateException("Redis timeout"))
            : Mono.empty());

    StepVerifier.create(producer.enqueueActive("project-1", "schema-1", 42L))
        .verifyComplete();

    verify(jobStore, times(2)).enqueueActive("project-1", "schema-1", 42L);
  }

  @Test
  @DisplayName("DELETED enqueue는 재시도 소진 후 Redis store 실패를 subscriber에게 전달한다")
  void deletedEnqueuePropagatesStoreFailureAfterRetriesExhausted() {
    IllegalStateException failure = new IllegalStateException("Redis down");
    given(jobStore.enqueueDeleted("project-1", "schema-1", 43L))
        .willReturn(Mono.error(failure));

    StepVerifier.create(
        producer.enqueueDeleted("project-1", "schema-1", 43L))
        .expectErrorSatisfies(error -> assertThat(error).hasCause(failure))
        .verify();

    verify(jobStore, times(3)).enqueueDeleted("project-1", "schema-1", 43L);
  }

}
