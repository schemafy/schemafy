package com.schemafy.domain.erd.constraint.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.constraint.application.port.out.CreateConstraintColumnPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintColumnsByConstraintIdPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintsByTableIdPort;
import com.schemafy.domain.erd.constraint.domain.Constraint;
import com.schemafy.domain.erd.constraint.domain.ConstraintColumn;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintColumnNotExistException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintPositionInvalidException;
import com.schemafy.domain.erd.constraint.domain.type.ConstraintKind;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddConstraintColumnService")
class AddConstraintColumnServiceTest {

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateConstraintColumnPort createConstraintColumnPort;

  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @Mock
  GetConstraintsByTableIdPort getConstraintsByTableIdPort;

  @Mock
  GetConstraintColumnsByConstraintIdPort getConstraintColumnsByConstraintIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @InjectMocks
  AddConstraintColumnService sut;

  @Nested
  @DisplayName("addConstraintColumn 메서드는")
  class AddConstraintColumn {

    @Test
    @DisplayName("유효한 요청에 대해 컬럼을 추가한다")
    void addsColumnWithValidRequest() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col2", 1);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(
          ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId(any()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));
      given(ulidGeneratorPort.generate())
          .willReturn("new-column-id");
      given(createConstraintColumnPort.createConstraintColumn(any(ConstraintColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.addConstraintColumn(command))
          .assertNext(result -> {
            assertThat(result.constraintColumnId()).isEqualTo("new-column-id");
            assertThat(result.constraintId()).isEqualTo("constraint1");
            assertThat(result.columnId()).isEqualTo("col2");
            assertThat(result.seqNo()).isEqualTo(1);
          })
          .verifyComplete();

      then(createConstraintColumnPort).should().createConstraintColumn(any(ConstraintColumn.class));
    }

    @Test
    @DisplayName("음수 seqNo면 예외가 발생한다")
    void throwsWhenNegativeSeqNo() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col1", -1);

      StepVerifier.create(sut.addConstraintColumn(command))
          .expectError(ConstraintPositionInvalidException.class)
          .verify();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("제약조건이 존재하지 않으면 예외가 발생한다")
    void throwsWhenConstraintNotExists() {
      var command = ConstraintFixture.addColumnCommand("nonexistent", "col1", 0);

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.addConstraintColumn(command))
          .expectError(RuntimeException.class)
          .verify();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("컬럼이 테이블에 존재하지 않으면 예외가 발생한다")
    void throwsWhenColumnNotExistsInTable() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "nonexistent", 0);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId(any()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.addConstraintColumn(command))
          .expectError(ConstraintColumnNotExistException.class)
          .verify();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("중복 컬럼 추가 시 예외가 발생한다")
    void throwsWhenDuplicateColumn() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col1", 1);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(
          ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId(any()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));

      StepVerifier.create(sut.addConstraintColumn(command))
          .expectError(ConstraintColumnDuplicateException.class)
          .verify();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("연속되지 않은 seqNo면 예외가 발생한다")
    void throwsWhenNonContiguousSeqNo() {
      var command = ConstraintFixture.addColumnCommand("constraint1", "col2", 5);
      var constraint = createConstraint("constraint1", "table1", ConstraintKind.PRIMARY_KEY);
      var existingColumns = List.of(
          ConstraintFixture.constraintColumn("cc1", "constraint1", "col1", 0));
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getColumnsByTableIdPort.findColumnsByTableId(any()))
          .willReturn(Mono.just(tableColumns));
      given(getConstraintsByTableIdPort.findConstraintsByTableId(any()))
          .willReturn(Mono.just(List.of(constraint)));
      given(getConstraintColumnsByConstraintIdPort.findConstraintColumnsByConstraintId(any()))
          .willReturn(Mono.just(existingColumns));

      StepVerifier.create(sut.addConstraintColumn(command))
          .expectError(ConstraintPositionInvalidException.class)
          .verify();

      then(createConstraintColumnPort).shouldHaveNoInteractions();
    }

  }

  private Constraint createConstraint(String id, String tableId, ConstraintKind kind) {
    return new Constraint(id, tableId, "test_constraint", kind, null, null);
  }

}
