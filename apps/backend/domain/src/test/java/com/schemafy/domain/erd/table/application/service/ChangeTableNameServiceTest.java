package com.schemafy.domain.erd.table.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.domain.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.ChangeRelationshipNamePort;
import com.schemafy.domain.erd.relationship.application.port.out.GetRelationshipsByTableIdPort;
import com.schemafy.domain.erd.relationship.application.port.out.RelationshipExistsPort;
import com.schemafy.domain.erd.relationship.domain.Relationship;
import com.schemafy.domain.erd.relationship.domain.type.Cardinality;
import com.schemafy.domain.erd.relationship.domain.type.RelationshipKind;
import com.schemafy.domain.erd.table.application.port.out.ChangeTableNamePort;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.application.port.out.TableExistsPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableNameDuplicateException;
import com.schemafy.domain.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeTableNameService")
class ChangeTableNameServiceTest {

  @Mock
  ChangeTableNamePort changeTableNamePort;

  @Mock
  TableExistsPort tableExistsPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetConstraintsByTableIdPort getConstraintsByTableIdPort;

  @Mock
  ChangeConstraintNamePort changeConstraintNamePort;

  @Mock
  ConstraintExistsPort constraintExistsPort;

  @Mock
  GetRelationshipsByTableIdPort getRelationshipsByTableIdPort;

  @Mock
  ChangeRelationshipNamePort changeRelationshipNamePort;

  @Mock
  RelationshipExistsPort relationshipExistsPort;

  @InjectMocks
  ChangeTableNameService sut;

  @Nested
  @DisplayName("changeTableName 메서드는")
  class ChangeTableName {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("테이블 이름을 변경한다")
      void changesTableName() {
        var command = TableFixture.changeNameCommand("new_table_name");

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(TableFixture.defaultTable()));
        given(changeTableNamePort.changeTableName(any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
            .willReturn(Mono.just(List.of()));
        given(getRelationshipsByTableIdPort.findRelationshipsByTableId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeTableName(command))
            .verifyComplete();

        then(tableExistsPort).should()
            .existsBySchemaIdAndName(command.schemaId(), command.newName());
        then(changeTableNamePort).should()
            .changeTableName(command.tableId(), command.newName());
      }

    }

    @Nested
    @DisplayName("자동 생성된 관계 이름이 있으면")
    class WithAutoRelationshipNames {

      @Test
      @DisplayName("테이블 rename에 맞춰 관계 이름을 갱신한다")
      void updatesAutoRelationshipName() {
        var oldTable = new Table(
            "table-1",
            "schema-1",
            "orders",
            TableFixture.DEFAULT_CHARSET,
            TableFixture.DEFAULT_COLLATION);
        var pkTable = new Table(
            "table-2",
            "schema-1",
            "users",
            TableFixture.DEFAULT_CHARSET,
            TableFixture.DEFAULT_COLLATION);
        var relationship = new Relationship(
            "rel-1",
            pkTable.id(),
            oldTable.id(),
            "rel_orders_to_users",
            RelationshipKind.NON_IDENTIFYING,
            Cardinality.ONE_TO_MANY,
            null);
        var command = new com.schemafy.domain.erd.table.application.port.in.ChangeTableNameCommand(
            oldTable.schemaId(),
            oldTable.id(),
            "orders_v2");

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getTableByIdPort.findTableById(oldTable.id()))
            .willReturn(Mono.just(oldTable));
        given(getTableByIdPort.findTableById(pkTable.id()))
            .willReturn(Mono.just(pkTable));
        given(changeTableNamePort.changeTableName(any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
            .willReturn(Mono.just(List.of()));
        given(getRelationshipsByTableIdPort.findRelationshipsByTableId(oldTable.id()))
            .willReturn(Mono.just(List.of(relationship)));
        given(relationshipExistsPort.existsByFkTableIdAndNameExcludingId(
            relationship.fkTableId(),
            "rel_orders_v2_to_users",
            relationship.id()))
            .willReturn(Mono.just(false));
        given(changeRelationshipNamePort.changeRelationshipName(any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeTableName(command))
            .verifyComplete();

        then(changeRelationshipNamePort).should()
            .changeRelationshipName(relationship.id(), "rel_orders_v2_to_users");
      }

    }

    @Nested
    @DisplayName("커스텀 관계 이름이 있으면")
    class WithCustomRelationshipNames {

      @Test
      @DisplayName("관계 이름을 갱신하지 않는다")
      void doesNotUpdateCustomRelationshipName() {
        var oldTable = new Table(
            "table-1",
            "schema-1",
            "orders",
            TableFixture.DEFAULT_CHARSET,
            TableFixture.DEFAULT_COLLATION);
        var pkTable = new Table(
            "table-2",
            "schema-1",
            "users",
            TableFixture.DEFAULT_CHARSET,
            TableFixture.DEFAULT_COLLATION);
        var relationship = new Relationship(
            "rel-1",
            pkTable.id(),
            oldTable.id(),
            "custom_rel_name",
            RelationshipKind.NON_IDENTIFYING,
            Cardinality.ONE_TO_MANY,
            null);
        var command = new com.schemafy.domain.erd.table.application.port.in.ChangeTableNameCommand(
            oldTable.schemaId(),
            oldTable.id(),
            "orders_v2");

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(false));
        given(getTableByIdPort.findTableById(oldTable.id()))
            .willReturn(Mono.just(oldTable));
        given(getTableByIdPort.findTableById(pkTable.id()))
            .willReturn(Mono.just(pkTable));
        given(changeTableNamePort.changeTableName(any(), any()))
            .willReturn(Mono.empty());
        given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
            .willReturn(Mono.just(List.of()));
        given(getRelationshipsByTableIdPort.findRelationshipsByTableId(oldTable.id()))
            .willReturn(Mono.just(List.of(relationship)));

        StepVerifier.create(sut.changeTableName(command))
            .verifyComplete();

        then(changeRelationshipNamePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("중복된 이름이 존재하면")
    class WithDuplicateName {

      @Test
      @DisplayName("TableNameDuplicateException을 발생시킨다")
      void throwsTableNameDuplicateException() {
        var command = TableFixture.changeNameCommand("existing_table_name");

        given(tableExistsPort.existsBySchemaIdAndName(any(), any()))
            .willReturn(Mono.just(true));

        StepVerifier.create(sut.changeTableName(command))
            .expectError(TableNameDuplicateException.class)
            .verify();

        then(changeTableNamePort).shouldHaveNoInteractions();
      }

    }

  }

}
