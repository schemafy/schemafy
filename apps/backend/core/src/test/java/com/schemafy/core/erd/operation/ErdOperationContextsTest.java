package com.schemafy.core.erd.operation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErdOperationContexts")
class ErdOperationContextsTest {

  @Test
  @DisplayName("metadata가 없으면 empty metadata를 반환한다")
  void returnsEmptyMetadataWhenContextMissing() {
    StepVerifier.create(Mono.deferContextual(ctx -> Mono.just(ErdOperationContexts.metadata(ctx))))
        .assertNext(metadata -> assertThat(metadata).isEqualTo(ErdOperationMetadata.empty()))
        .verifyComplete();
  }

  @Test
  @DisplayName("여러 metadata writer가 순서대로 누적된다")
  void mergesMetadataAcrossMultipleContextWrites() {
    StepVerifier.create(Mono.deferContextual(ctx -> Mono.just(ErdOperationContexts.metadata(ctx)))
        .contextWrite(ErdOperationContexts.withDerivedFromOpId("op-1"))
        .contextWrite(ErdOperationContexts.withDerivationKind(ErdOperationDerivationKind.UNDO))
        .contextWrite(ErdOperationContexts.withActorUserId("user-1"))
        .contextWrite(ErdOperationContexts.withBaseSchemaRevision(7L))
        .contextWrite(ErdOperationContexts.withClientOperationId("client-op-1"))
        .contextWrite(ErdOperationContexts.withSessionId("session-1")))
        .assertNext(metadata -> assertThat(metadata).isEqualTo(new ErdOperationMetadata(
            "session-1",
            "client-op-1",
            7L,
            "user-1",
            ErdOperationDerivationKind.UNDO,
            "op-1")))
        .verifyComplete();
  }

  @Test
  @DisplayName("suppression flag는 metadata와 독립적으로 동작한다")
  void keepsSuppressionFlagIndependentFromMetadata() {
    StepVerifier.create(Mono.deferContextual(ctx -> Mono.just(new ContextSnapshot(
        ErdOperationContexts.metadata(ctx),
        ErdOperationContexts.isNestedMutationSuppressed(ctx))))
        .contextWrite(ErdOperationContexts.suppressNestedMutation())
        .contextWrite(ErdOperationContexts.withSessionId("session-1")))
        .assertNext(snapshot -> {
          assertThat(snapshot.metadata()).isEqualTo(new ErdOperationMetadata(
              "session-1",
              null,
              null,
              null,
              null,
              null));
          assertThat(snapshot.suppressed()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("typed suppression flag가 없으면 false를 반환한다")
  void returnsFalseWhenSuppressionFlagMissing() {
    StepVerifier.create(Mono.deferContextual(ctx -> Mono.just(ErdOperationContexts.isNestedMutationSuppressed(ctx))))
        .expectNext(false)
        .verifyComplete();
  }

  private record ContextSnapshot(
      ErdOperationMetadata metadata,
      boolean suppressed) {
  }

}
