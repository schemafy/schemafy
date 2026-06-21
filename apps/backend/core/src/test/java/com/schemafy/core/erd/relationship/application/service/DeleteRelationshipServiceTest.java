package com.schemafy.core.erd.relationship.application.service;

import java.util.List;

import org.springframework.transaction.reactive.TransactionalOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.erd.column.application.port.in.DeleteColumnCommand;
import com.schemafy.core.erd.column.application.port.in.DeleteColumnUseCase;
import com.schemafy.core.erd.operation.application.service.NestedErdMutations;
import com.schemafy.core.erd.operation.application.service.StructuralSnapshotService;
import com.schemafy.core.erd.relationship.application.port.out.DeleteRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.application.port.out.DeleteRelationshipPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipColumnsByRelationshipIdPort;
import com.schemafy.core.erd.relationship.domain.RelationshipColumn;
import com.schemafy.core.erd.relationship.fixture.RelationshipFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.schemafy.core.erd.operation.application.service.StructuralSnapshotServiceTestSupport.stubEmptySnapshots;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteRelationshipService")
class DeleteRelationshipServiceTest {

  @Mock
  TransactionalOperator transactionalOperator;

  @Mock
  DeleteRelationshipPort deleteRelationshipPort;

  @Mock
  DeleteRelationshipColumnsByRelationshipIdPort deleteRelationshipColumnsPort;

  @Mock
  GetRelationshipColumnsByRelationshipIdPort getRelationshipColumnsByRelationshipIdPort;

  @Mock
  DeleteColumnUseCase deleteColumnUseCase;

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @Mock
  StructuralSnapshotService structuralSnapshotService;

  @InjectMocks
  DeleteRelationshipService sut;

  @BeforeEach
  void setUp() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    stubEmptySnapshots(structuralSnapshotService);
  }

  @Nested
  @DisplayName("deleteRelationship 메서드는")
  class DeleteRelationship {

    @Test
    @DisplayName("관계 컬럼을 먼저 삭제 후 관계를 삭제하고 FK 컬럼도 삭제한다")
    void deletesColumnsFirstThenRelationshipThenFkColumns() {
      var command = RelationshipFixture.deleteCommand();
      var relColumns = List.of(
          new RelationshipColumn("rc1", RelationshipFixture.DEFAULT_ID, "pk1", "fk1", 0));

      given(getRelationshipByIdPort.findRelationshipById(RelationshipFixture.DEFAULT_ID))
          .willReturn(Mono.just(RelationshipFixture.defaultRelationship()));
      given(getRelationshipColumnsByRelationshipIdPort
          .findRelationshipColumnsByRelationshipId(RelationshipFixture.DEFAULT_ID))
          .willReturn(Mono.just(relColumns));
      given(deleteRelationshipColumnsPort.deleteByRelationshipId(eq(RelationshipFixture.DEFAULT_ID)))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(eq(RelationshipFixture.DEFAULT_ID)))
          .willReturn(Mono.empty());
      given(deleteColumnUseCase.deleteColumn(any(DeleteColumnCommand.class)))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteRelationship(command))
          .expectNextCount(1)
          .verifyComplete();

      var inOrderVerifier = inOrder(
          deleteRelationshipColumnsPort, deleteRelationshipPort, deleteColumnUseCase);
      inOrderVerifier.verify(deleteRelationshipColumnsPort)
          .deleteByRelationshipId(RelationshipFixture.DEFAULT_ID);
      inOrderVerifier.verify(deleteRelationshipPort)
          .deleteRelationship(RelationshipFixture.DEFAULT_ID);
      inOrderVerifier.verify(deleteColumnUseCase)
          .deleteColumn(eq(new DeleteColumnCommand("fk1")));
    }

    @Test
    @DisplayName("관계 컬럼이 없으면 FK 컬럼 삭제 없이 관계만 삭제한다")
    void deletesRelationshipWithoutFkColumnsWhenNoRelColumns() {
      var command = RelationshipFixture.deleteCommand();

      given(getRelationshipByIdPort.findRelationshipById(RelationshipFixture.DEFAULT_ID))
          .willReturn(Mono.just(RelationshipFixture.defaultRelationship()));
      given(getRelationshipColumnsByRelationshipIdPort
          .findRelationshipColumnsByRelationshipId(RelationshipFixture.DEFAULT_ID))
          .willReturn(Mono.just(List.of()));
      given(deleteRelationshipColumnsPort.deleteByRelationshipId(eq(RelationshipFixture.DEFAULT_ID)))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(eq(RelationshipFixture.DEFAULT_ID)))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.deleteRelationship(command))
          .expectNextCount(1)
          .verifyComplete();

      then(deleteColumnUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("nested mutation runner로 실행하면 snapshot 없이 관계를 삭제한다")
    void skipsSnapshotsWhenNestedMutationSuppressed() {
      var command = RelationshipFixture.deleteCommand();

      given(getRelationshipByIdPort.findRelationshipById(RelationshipFixture.DEFAULT_ID))
          .willReturn(Mono.just(RelationshipFixture.defaultRelationship()));
      given(getRelationshipColumnsByRelationshipIdPort
          .findRelationshipColumnsByRelationshipId(RelationshipFixture.DEFAULT_ID))
          .willReturn(Mono.just(List.of()));
      given(deleteRelationshipColumnsPort.deleteByRelationshipId(eq(RelationshipFixture.DEFAULT_ID)))
          .willReturn(Mono.empty());
      given(deleteRelationshipPort.deleteRelationship(eq(RelationshipFixture.DEFAULT_ID)))
          .willReturn(Mono.empty());

      StepVerifier.create(NestedErdMutations.run(sut.deleteRelationship(command)))
          .expectNextCount(1)
          .verifyComplete();

      then(deleteRelationshipPort).should()
          .deleteRelationship(RelationshipFixture.DEFAULT_ID);
      then(structuralSnapshotService).should(never()).captureByRelationshipId(any());
      then(structuralSnapshotService).should(never()).captureBySchemaId(any());
    }

  }

}
