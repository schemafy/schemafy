package com.schemafy.core.erd.operation.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.common.MutationResult;
import com.schemafy.core.common.json.JsonCodec;
import com.schemafy.core.erd.operation.application.port.out.AppendErdOperationLogPort;
import com.schemafy.core.erd.operation.application.port.out.FindSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.application.port.out.IncrementSchemaCollaborationRevisionPort;
import com.schemafy.core.erd.operation.application.port.out.SaveSchemaCollaborationStatePort;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;
import com.schemafy.core.erd.operation.domain.ErdOperationLifecycleState;
import com.schemafy.core.erd.operation.domain.ErdOperationLog;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.operation.domain.SchemaCollaborationState;
import com.schemafy.core.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DefaultErdMutationCoordinatorTest {

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  ErdMutationTargetResolver erdMutationTargetResolver;

  @Mock
  ErdMutationTargetFinalizer erdMutationTargetFinalizer;

  @Mock
  FindSchemaCollaborationStatePort findSchemaCollaborationStatePort;

  @Mock
  IncrementSchemaCollaborationRevisionPort incrementSchemaCollaborationRevisionPort;

  @Mock
  SaveSchemaCollaborationStatePort saveSchemaCollaborationStatePort;

  @Mock
  AppendErdOperationLogPort appendErdOperationLogPort;

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  JsonCodec jsonCodec;

  DefaultErdMutationCoordinator sut;

  @BeforeEach
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void setUp() {
    sut = new DefaultErdMutationCoordinator(
        transactionalOperator,
        erdMutationTargetResolver,
        erdMutationTargetFinalizer,
        findSchemaCollaborationStatePort,
        incrementSchemaCollaborationRevisionPort,
        saveSchemaCollaborationStatePort,
        appendErdOperationLogPort,
        ulidGeneratorPort,
        jsonCodec);

    lenient().when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(jsonCodec.serialize(any())).thenReturn("{}");
    lenient().when(ulidGeneratorPort.generate()).thenReturn("operation1");
  }

  @Test
  @DisplayName("top-level mutation은 schema state row lock을 잡은 뒤 mutation을 실행한다")
  void executesTopLevelMutationAfterLockingSchemaState() {
    Object payload = new Object();
    ResolvedErdMutationTarget resolvedTarget = new ResolvedErdMutationTarget(
        "project1",
        "schema1",
        "table1");
    SchemaCollaborationState lockedState = new SchemaCollaborationState(
        "schema1",
        "project1",
        3L,
        null,
        null);
    SchemaCollaborationState updatedState = new SchemaCollaborationState(
        "schema1",
        "project1",
        4L,
        null,
        null);
    FinalizedErdMutationTarget finalizedTarget = new FinalizedErdMutationTarget(
        "project1",
        "schema1");
    List<String> events = new ArrayList<>();

    given(erdMutationTargetResolver.resolveBefore(ErdOperationType.CREATE_TABLE, payload))
        .willReturn(Mono.just(resolvedTarget));
    given(findSchemaCollaborationStatePort.findBySchemaIdForUpdate("schema1"))
        .willAnswer(invocation -> {
          events.add("lock");
          return Mono.just(lockedState);
        });
    given(erdMutationTargetFinalizer.finalizeTarget(eq(ErdOperationType.CREATE_TABLE),
        eq(resolvedTarget), any()))
        .willReturn(finalizedTarget);
    given(incrementSchemaCollaborationRevisionPort.increment("schema1"))
        .willReturn(Mono.just(updatedState));
    given(appendErdOperationLogPort.append(any(ErdOperationLog.class)))
        .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    Supplier<Mono<MutationResult<String>>> mutationSupplier = () -> {
      events.add("mutate");
      return Mono.just(MutationResult.of("ok", Set.of("table1")));
    };

    StepVerifier.create(sut.coordinate(ErdOperationType.CREATE_TABLE, payload,
        mutationSupplier))
        .assertNext(result -> {
          assertThat(result.result()).isEqualTo("ok");
          assertThat(result.operation()).isEqualTo(new CommittedErdOperation(
              "operation1",
              null,
              4L,
              ErdOperationDerivationKind.ORIGINAL));
        })
        .verifyComplete();

    assertThat(events).containsExactly("lock", "mutate");
    then(findSchemaCollaborationStatePort).should()
        .findBySchemaIdForUpdate("schema1");
    then(findSchemaCollaborationStatePort).should(never())
        .findBySchemaId("schema1");
    then(appendErdOperationLogPort).should().append(new ErdOperationLog(
        "operation1",
        "project1",
        "schema1",
        ErdOperationType.CREATE_TABLE,
        4L,
        null,
        null,
        null,
        "system",
        ErdOperationDerivationKind.ORIGINAL,
        null,
        ErdOperationLifecycleState.COMMITTED,
        "{}",
        null,
        "{}"));
  }

}
