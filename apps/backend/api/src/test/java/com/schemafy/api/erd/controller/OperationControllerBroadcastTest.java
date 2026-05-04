package com.schemafy.api.erd.controller;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.erd.broadcast.ErdMutationBroadcaster;
import com.schemafy.core.common.MutationResult;
import com.schemafy.core.erd.operation.application.inverse.CreateTableInverse;
import com.schemafy.core.erd.operation.application.inverse.StructuralSnapshot;
import com.schemafy.core.erd.operation.application.port.in.RedoErdOperationUseCase;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationCommand;
import com.schemafy.core.erd.operation.application.port.in.UndoErdOperationUseCase;
import com.schemafy.core.erd.operation.domain.CommittedErdOperation;
import com.schemafy.core.erd.operation.domain.ErdOperationDerivationKind;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperationController 브로드캐스트 단위 테스트")
class OperationControllerBroadcastTest {

  @Mock
  UndoErdOperationUseCase undoErdOperationUseCase;

  @Mock
  RedoErdOperationUseCase redoErdOperationUseCase;

  @Mock
  ObjectProvider<ErdMutationBroadcaster> broadcasterProvider;

  @Mock
  ErdMutationBroadcaster broadcaster;

  OperationController sut;

  @BeforeEach
  void setUp() {
    sut = new OperationController(
        undoErdOperationUseCase,
        redoErdOperationUseCase,
        broadcasterProvider);
  }

  @Test
  @DisplayName("structural undo는 삭제된 table id 대신 schema context로 브로드캐스트한다")
  void structuralUndoBroadcastsWithSchemaContext() {
    String originalOpId = "original-op-1";
    String schemaId = "schema-1";
    String deletedTableId = "deleted-table";
    Set<String> affectedTableIds = Set.of(deletedTableId);
    CommittedErdOperation undoOperation = new CommittedErdOperation(
        "undo-op-1",
        null,
        2L,
        ErdOperationDerivationKind.UNDO);
    CreateTableInverse inverse = new CreateTableInverse(
        schemaId,
        deletedTableId,
        emptySnapshot(schemaId),
        emptySnapshot(schemaId),
        List.of(deletedTableId));

    given(broadcasterProvider.getIfAvailable()).willReturn(broadcaster);
    given(undoErdOperationUseCase.undo(new UndoErdOperationCommand(originalOpId)))
        .willReturn(Mono.just(MutationResult.<Void>of(null, affectedTableIds)
            .withInverse(inverse)
            .withOperation(undoOperation)));
    given(broadcaster.broadcastSchemaMutation(schemaId, affectedTableIds,
        undoOperation))
        .willReturn(Mono.empty());

    StepVerifier.create(sut.undo(originalOpId))
        .assertNext(response -> {
          assertThat(response.affectedTableIds()).containsExactly(deletedTableId);
          assertThat(response.operation()).isEqualTo(undoOperation);
        })
        .verifyComplete();

    then(broadcaster).should().broadcastSchemaMutation(schemaId,
        affectedTableIds, undoOperation);
    then(broadcaster).should(never()).broadcast(any(), any());
  }

  private static StructuralSnapshot emptySnapshot(String schemaId) {
    return new StructuralSnapshot(
        schemaId,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

}
