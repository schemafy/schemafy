package com.schemafy.core.erd.operation.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.core.erd.column.application.port.in.ChangeColumnTypeCommand;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.domain.Column;
import com.schemafy.core.erd.column.domain.ColumnTypeArguments;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.core.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.core.erd.constraint.domain.Constraint;
import com.schemafy.core.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.core.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.core.erd.operation.domain.ErdOperationType;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipByIdPort;
import com.schemafy.core.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.core.erd.relationship.domain.Relationship;
import com.schemafy.core.erd.relationship.domain.type.Cardinality;
import com.schemafy.core.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.domain.Table;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SameCommandInversePayloadRegistry")
class SameCommandInversePayloadRegistryTest {

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetConstraintsByTableIdPort getConstraintsByTableIdPort;

  @Mock
  GetRelationshipsByTableIdPort getRelationshipsByTableIdPort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  GetRelationshipByIdPort getRelationshipByIdPort;

  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @Mock
  GetIndexByIdPort getIndexByIdPort;

  @InjectMocks
  SameCommandInversePayloadRegistry sut;

  @Test
  @DisplayName("table name inverse payload는 PK와 자동 관계 이름 복원 정보를 저장한다")
  void resolvesInversePayloadForTableNameWithSideEffectNameRestores() {
    Table orders = new Table("table-1", "schema-1", "orders", "utf8mb4", "utf8mb4_general_ci");
    Table users = new Table("table-2", "schema-1", "users", "utf8mb4", "utf8mb4_general_ci");
    Constraint primaryKey = new Constraint(
        "constraint-1",
        orders.id(),
        "primary_orders",
        ConstraintKind.PRIMARY_KEY,
        null,
        null);
    Relationship autoRelationship = new Relationship(
        "relationship-1",
        users.id(),
        orders.id(),
        "rel_orders_to_users",
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        null);
    Relationship customRelationship = new Relationship(
        "relationship-2",
        users.id(),
        orders.id(),
        "custom_relationship",
        RelationshipKind.NON_IDENTIFYING,
        Cardinality.ONE_TO_MANY,
        null);

    given(getTableByIdPort.findTableById(orders.id()))
        .willReturn(Mono.just(orders));
    given(getTableByIdPort.findTableById(users.id()))
        .willReturn(Mono.just(users));
    given(getConstraintsByTableIdPort.findConstraintsByTableId(orders.id()))
        .willReturn(Mono.just(List.of(primaryKey)));
    given(getRelationshipsByTableIdPort.findRelationshipsByTableId(orders.id()))
        .willReturn(Mono.just(List.of(autoRelationship, customRelationship)));

    StepVerifier.create(sut.resolve(
        ErdOperationType.CHANGE_TABLE_NAME,
        new ChangeTableNameCommand(orders.id(), "orders_v2")))
        .assertNext(inverse -> assertThat(inverse).isEqualTo(new ChangeTableNameReplayPayload(
            orders.id(),
            orders.name(),
            List.of(new ChangeTableNameReplayPayload.NameRestore(primaryKey.id(), primaryKey.name())),
            List.of(new ChangeTableNameReplayPayload.NameRestore(autoRelationship.id(), autoRelationship.name())))))
        .verifyComplete();
  }

  @Test
  @DisplayName("column type inverse payload는 기존 type arguments를 모두 복원한다")
  void resolvesInversePayloadForColumnType() {
    Column column = new Column(
        "column-1",
        "table-1",
        "status",
        "ENUM",
        new ColumnTypeArguments(null, null, null, List.of("READY", "DONE")),
        0,
        false,
        null,
        null,
        null);

    given(getColumnByIdPort.findColumnById("column-1"))
        .willReturn(Mono.just(column));

    StepVerifier.create(sut.resolve(
        ErdOperationType.CHANGE_COLUMN_TYPE,
        new ChangeColumnTypeCommand("column-1", "INT", null, null, null)))
        .assertNext(inverse -> assertThat(inverse).isEqualTo(new ChangeColumnTypeCommand(
            "column-1",
            "ENUM",
            null,
            null,
            null,
            List.of("READY", "DONE"))))
        .verifyComplete();
  }

  @Test
  @DisplayName("등록되지 않은 연산 inverse payload는 비어 있다")
  void returnsEmptyForUnsupportedOperation() {
    StepVerifier.create(sut.resolve(ErdOperationType.DELETE_TABLE, new Object()))
        .verifyComplete();
  }

  @Test
  @DisplayName("relationship kind 변경은 same-command inverse payload 대상으로 등록하지 않는다")
  void returnsEmptyForRelationshipKind() {
    StepVerifier.create(sut.resolve(ErdOperationType.CHANGE_RELATIONSHIP_KIND, new Object()))
        .verifyComplete();
  }

}
