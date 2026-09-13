package com.schemafy.core.erd.operation.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.erd.operation.ErdOperationContexts;
import com.schemafy.core.erd.operation.ErdOperationMetadata;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NestedErdMutations")
class NestedErdMutationsTest {

  @Test
  @DisplayName("nested mutation suppression flag를 붙여 실행한다")
  void runsWithNestedMutationSuppression() {
    StepVerifier.create(NestedErdMutations.run(Mono.deferContextual(contextView -> Mono.just(
        ErdOperationContexts.isNestedMutationSuppressed(contextView)))))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  @DisplayName("기존 operation metadata를 유지한다")
  void keepsExistingOperationMetadata() {
    StepVerifier.create(NestedErdMutations.run(Mono.deferContextual(contextView -> Mono.just(new ContextSnapshot(
        ErdOperationContexts.metadata(contextView),
        ErdOperationContexts.isNestedMutationSuppressed(contextView)))))
        .contextWrite(ErdOperationContexts.withSessionId("session-1")))
        .assertNext(snapshot -> {
          assertThat(snapshot.metadata()).isEqualTo(new ErdOperationMetadata(
              "session-1",
              null,
              null,
              null));
          assertThat(snapshot.suppressed()).isTrue();
        })
        .verifyComplete();
  }

  private record ContextSnapshot(
      ErdOperationMetadata metadata,
      boolean suppressed) {
  }

}
