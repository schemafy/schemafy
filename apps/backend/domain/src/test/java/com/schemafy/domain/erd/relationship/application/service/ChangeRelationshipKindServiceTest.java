package com.schemafy.domain.erd.relationship.application.service;

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

import com.schemafy.domain.common.exception.InvalidValueException;
import com.schemafy.domain.erd.constraint.application.service.PkCascadeHelper;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipKindPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsBySchemaIdPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipCyclicReferenceException;
import com.schemafy.domain.erd.relationship.domain.exception.RelationshipNotExistException;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.relationship.fixture.RelationshipFixture;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeRelationshipKindService")
class ChangeRelationshipKindServiceTest {

  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";

  @Mock
  ChangeRelationshipKindPort changeRelationshipKindPort;

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetRelationshipsBySchemaIdPort getRelationshipsBySchemaIdPort;

  @Mock
  PkCascadeHelper pkCascadeHelper;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  ChangeRelationshipKindService sut;

  @BeforeEach
  void setUpTransaction() {
    lenient().when(transactionalOperator.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("changeRelationshipKind 메서드는")
  class ChangeRelationshipKind {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("NON_IDENTIFYING에서 IDENTIFYING으로 변경한다")
      void changesFromNonIdentifyingToIdentifying() {
        var command = RelationshipFixture.changeKindCommand(RelationshipKind.IDENTIFYING);
        var relationship = RelationshipFixture.nonIdentifyingRelationship();
        var table = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);

        given(getRelationshipByIdPort.findRelationshipById(any()))
            .willReturn(Mono.just(relationship));
        given(getTableByIdPort.findTableById(relationship.fkTableId()))
            .willReturn(Mono.just(table));
        given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(SCHEMA_ID))
            .willReturn(Mono.just(List.of(relationship)));
        given(pkCascadeHelper.syncPkForKindChange(
            eq(relationship),
            eq(RelationshipKind.NON_IDENTIFYING),
            eq(RelationshipKind.IDENTIFYING),
            anySet(),
            anySet()))
            .willReturn(Mono.empty());
        given(changeRelationshipKindPort.changeRelationshipKind(any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeRelationshipKind(command))
            .expectNextCount(1)
            .verifyComplete();

        then(pkCascadeHelper).should().syncPkForKindChange(
            eq(relationship),
            eq(RelationshipKind.NON_IDENTIFYING),
            eq(RelationshipKind.IDENTIFYING),
            anySet(),
            anySet());
        then(changeRelationshipKindPort).should()
            .changeRelationshipKind(eq(relationship.id()), eq(RelationshipKind.IDENTIFYING));
      }

      @Test
      @DisplayName("IDENTIFYING에서 NON_IDENTIFYING으로 변경한다")
      void changesFromIdentifyingToNonIdentifying() {
        var command = RelationshipFixture.changeKindCommand(RelationshipKind.NON_IDENTIFYING);
        var relationship = RelationshipFixture.identifyingRelationship();

        given(getRelationshipByIdPort.findRelationshipById(any()))
            .willReturn(Mono.just(relationship));
        given(pkCascadeHelper.syncPkForKindChange(
            eq(relationship),
            eq(RelationshipKind.IDENTIFYING),
            eq(RelationshipKind.NON_IDENTIFYING),
            anySet(),
            anySet()))
            .willReturn(Mono.empty());
        given(changeRelationshipKindPort.changeRelationshipKind(any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeRelationshipKind(command))
            .expectNextCount(1)
            .verifyComplete();

        then(pkCascadeHelper).should().syncPkForKindChange(
            eq(relationship),
            eq(RelationshipKind.IDENTIFYING),
            eq(RelationshipKind.NON_IDENTIFYING),
            anySet(),
            anySet());
        then(changeRelationshipKindPort).should()
            .changeRelationshipKind(eq(relationship.id()), eq(RelationshipKind.NON_IDENTIFYING));
      }

    }

    @Nested
    @DisplayName("관계가 존재하지 않으면")
    class WhenRelationshipNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = RelationshipFixture.changeKindCommand(RelationshipKind.IDENTIFYING);

        given(getRelationshipByIdPort.findRelationshipById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeRelationshipKind(command))
            .expectError(RelationshipNotExistException.class)
            .verify();

        then(changeRelationshipKindPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("kind가 null이면")
    class WhenKindIsNull {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = RelationshipFixture.changeKindCommand(
            RelationshipFixture.DEFAULT_ID, null);

        StepVerifier.create(sut.changeRelationshipKind(command))
            .expectError(InvalidValueException.class)
            .verify();

        then(changeRelationshipKindPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("NON_IDENTIFYING에서 IDENTIFYING으로 변경 시 순환이 발생하면")
    class WhenChangingToIdentifyingCreatesCycle {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = RelationshipFixture.changeKindCommand(RelationshipKind.IDENTIFYING);
        var relationship = RelationshipFixture.nonIdentifyingRelationship();
        var reverseRelationship = new Relationship(
            "reverse",
            RelationshipFixture.DEFAULT_FK_TABLE_ID,
            RelationshipFixture.DEFAULT_PK_TABLE_ID,
            "fk_reverse",
            RelationshipKind.IDENTIFYING,
            Cardinality.ONE_TO_MANY,
            null);
        var table = createTable(RelationshipFixture.DEFAULT_FK_TABLE_ID, SCHEMA_ID);

        given(getRelationshipByIdPort.findRelationshipById(any()))
            .willReturn(Mono.just(relationship));
        given(getTableByIdPort.findTableById(relationship.fkTableId()))
            .willReturn(Mono.just(table));
        given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(SCHEMA_ID))
            .willReturn(Mono.just(List.of(relationship, reverseRelationship)));

        StepVerifier.create(sut.changeRelationshipKind(command))
            .expectError(RelationshipCyclicReferenceException.class)
            .verify();

        then(changeRelationshipKindPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("간접 순환이 발생하면")
    class WhenIndirectCycleOccurs {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        String tableA = "tableA";
        String tableB = "tableB";
        String tableC = "tableC";
        var command = RelationshipFixture.changeKindCommand("relAB", RelationshipKind.IDENTIFYING);
        var relationshipAB = new Relationship(
            "relAB", tableA, tableB, "fk_a_b",
            RelationshipKind.NON_IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var relationshipBC = new Relationship(
            "relBC", tableB, tableC, "fk_b_c",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var relationshipCA = new Relationship(
            "relCA", tableC, tableA, "fk_c_a",
            RelationshipKind.IDENTIFYING, Cardinality.ONE_TO_MANY, null);
        var table = createTable(tableB, SCHEMA_ID);

        given(getRelationshipByIdPort.findRelationshipById("relAB"))
            .willReturn(Mono.just(relationshipAB));
        given(getTableByIdPort.findTableById(tableB))
            .willReturn(Mono.just(table));
        given(getRelationshipsBySchemaIdPort.findRelationshipsBySchemaId(SCHEMA_ID))
            .willReturn(Mono.just(List.of(relationshipAB, relationshipBC, relationshipCA)));

        StepVerifier.create(sut.changeRelationshipKind(command))
            .expectError(RelationshipCyclicReferenceException.class)
            .verify();

        then(changeRelationshipKindPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("IDENTIFYING에서 NON_IDENTIFYING으로 변경 시 순환이 해제되면")
    class WhenChangingToNonIdentifyingBreaksCycle {

      @Test
      @DisplayName("통과한다")
      void passesAfterBreakingCycle() {
        var command = RelationshipFixture.changeKindCommand(RelationshipKind.NON_IDENTIFYING);
        var relationship = RelationshipFixture.identifyingRelationship();

        given(getRelationshipByIdPort.findRelationshipById(any()))
            .willReturn(Mono.just(relationship));
        given(pkCascadeHelper.syncPkForKindChange(
            eq(relationship),
            eq(RelationshipKind.IDENTIFYING),
            eq(RelationshipKind.NON_IDENTIFYING),
            anySet(),
            anySet()))
            .willReturn(Mono.empty());
        given(changeRelationshipKindPort.changeRelationshipKind(any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeRelationshipKind(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeRelationshipKindPort).should()
            .changeRelationshipKind(eq(relationship.id()), eq(RelationshipKind.NON_IDENTIFYING));
      }

    }

  }

  private Table createTable(String tableId, String schemaId) {
    return new Table(tableId, schemaId, "test_table", "utf8mb4", "utf8mb4_general_ci");
  }

}
