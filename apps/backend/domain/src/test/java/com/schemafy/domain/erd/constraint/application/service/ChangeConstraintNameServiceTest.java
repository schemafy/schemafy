package com.schemafy.domain.erd.constraint.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.constraint.application.port.out.ChangeConstraintNamePort;
import com.schemafy.domain.erd.constraint.application.port.out.ConstraintExistsPort;
import com.schemafy.domain.erd.constraint.application.port.out.GetConstraintByIdPort;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNameDuplicateException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNameInvalidException;
import com.schemafy.domain.erd.constraint.domain.exception.ConstraintNotExistException;
import com.schemafy.domain.erd.constraint.fixture.ConstraintFixture;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeConstraintNameService")
class ChangeConstraintNameServiceTest {

  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";

  @Mock
  ChangeConstraintNamePort changeConstraintNamePort;

  @Mock
  ConstraintExistsPort constraintExistsPort;

  @Mock
  GetConstraintByIdPort getConstraintByIdPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @InjectMocks
  ChangeConstraintNameService sut;

  @Nested
  @DisplayName("changeConstraintName 메서드는")
  class ChangeConstraintName {

    @Test
    @DisplayName("유효한 새 이름으로 변경한다")
    void changesNameWithValidNewName() {
      var command = ConstraintFixture.changeNameCommand("new_constraint_name");
      var constraint = ConstraintFixture.defaultConstraint();
      var table = createTable(constraint.tableId(), SCHEMA_ID);

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getTableByIdPort.findTableById(any()))
          .willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndNameExcludingId(any(), any(), any()))
          .willReturn(Mono.just(false));
      given(changeConstraintNamePort.changeConstraintName(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintName(command))
          .verifyComplete();

      then(changeConstraintNamePort).should()
          .changeConstraintName(eq(constraint.id()), eq("new_constraint_name"));
    }

    @Test
    @DisplayName("빈 이름이면 예외가 발생한다")
    void throwsWhenEmptyName() {
      var command = ConstraintFixture.changeNameCommand("");

      StepVerifier.create(sut.changeConstraintName(command))
          .expectError(ConstraintNameInvalidException.class)
          .verify();

      then(changeConstraintNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("공백 이름이면 예외가 발생한다")
    void throwsWhenBlankName() {
      var command = ConstraintFixture.changeNameCommand("   ");

      StepVerifier.create(sut.changeConstraintName(command))
          .expectError(ConstraintNameInvalidException.class)
          .verify();

      then(changeConstraintNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("제약조건이 존재하지 않으면 예외가 발생한다")
    void throwsWhenConstraintNotExists() {
      var command = ConstraintFixture.changeNameCommand("new_name");

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeConstraintName(command))
          .expectError(ConstraintNotExistException.class)
          .verify();

      then(changeConstraintNamePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이름이 중복되면 예외가 발생한다")
    void throwsWhenNameDuplicate() {
      var command = ConstraintFixture.changeNameCommand("duplicate_name");
      var constraint = ConstraintFixture.defaultConstraint();
      var table = createTable(constraint.tableId(), SCHEMA_ID);

      given(getConstraintByIdPort.findConstraintById(any()))
          .willReturn(Mono.just(constraint));
      given(getTableByIdPort.findTableById(any()))
          .willReturn(Mono.just(table));
      given(constraintExistsPort.existsBySchemaIdAndNameExcludingId(any(), any(), any()))
          .willReturn(Mono.just(true));

      StepVerifier.create(sut.changeConstraintName(command))
          .expectError(ConstraintNameDuplicateException.class)
          .verify();

      then(changeConstraintNamePort).shouldHaveNoInteractions();
    }

  }

  private Table createTable(String tableId, String schemaId) {
    return new Table(tableId, schemaId, "test_table", "utf8mb4", "utf8mb4_general_ci");
  }

}
