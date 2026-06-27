package com.schemafy.core.erd.column.application.service;

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

import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.column.application.port.out.ChangeColumnNamePort;
import com.schemafy.core.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.core.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.core.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.core.erd.column.fixture.ColumnFixture;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.fixture.SchemaFixture;
import com.schemafy.core.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.core.erd.table.fixture.TableFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeColumnNameService")
class ChangeColumnNameServiceTest {

  @Mock
  ChangeColumnNamePort changeColumnNamePort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  ChangeColumnNameService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("changeColumnName 메서드는")
  class ChangeColumnName {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("컬럼 이름을 변경한다")
      void changesColumnName() {
        var newName = "new_column_name";
        var command = ColumnFixture.changeNameCommand(newName);
        var column = ColumnFixture.defaultColumn();
        var table = TableFixture.defaultTable();
        var schema = SchemaFixture.defaultSchema();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnNamePort.changeColumnName(any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnName(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnNamePort).should()
            .changeColumnName(command.columnId(), newName);
      }

      @Test
      @DisplayName("현재 이름과 같으면 주변 context 조회 없이 변경 없이 성공한다")
      void succeedsWithoutContextLookupWhenNameIsSame() {
        var command = ColumnFixture.changeNameCommand(ColumnFixture.DEFAULT_NAME);
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(command.columnId()))
            .willReturn(Mono.just(column));

        StepVerifier.create(sut.changeColumnName(command))
            .expectNextMatches(result -> result.operation() == null)
            .verifyComplete();

        then(getTableByIdPort).shouldHaveNoInteractions();
        then(getSchemaByIdPort).shouldHaveNoInteractions();
        then(getColumnsByTableIdPort).shouldHaveNoInteractions();
        then(changeColumnNamePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("컬럼이 존재하지 않으면")
    class WhenColumnNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = ColumnFixture.changeNameCommand("new_name");

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnName(command))
            .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.NOT_FOUND))
            .verify();

        then(changeColumnNamePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("중복된 이름이 존재하면")
    class WithDuplicateName {

      @Test
      @DisplayName("ColumnNameDuplicateException이 발생한다")
      void throwsColumnNameDuplicateException() {
        var command = ColumnFixture.changeNameCommand("existing_column");
        var column = ColumnFixture.defaultColumn();
        var existingColumn = ColumnFixture.columnWithIdAndName(
            "01ARZ3NDEKTSV4RRFFQ69G5EXS", "existing_column");
        var columns = List.of(column, existingColumn);
        var table = TableFixture.defaultTable();
        var schema = SchemaFixture.defaultSchema();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(columns));

        StepVerifier.create(sut.changeColumnName(command))
            .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.NAME_DUPLICATE))
            .verify();

        then(changeColumnNamePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("예약어 이름이면")
    class WithReservedKeywordName {

      @Test
      @DisplayName("ColumnNameReservedException이 발생한다")
      void throwsColumnNameReservedException() {
        var command = ColumnFixture.changeNameCommand("SELECT");
        var column = ColumnFixture.defaultColumn();
        var table = TableFixture.defaultTable();
        var schema = SchemaFixture.defaultSchema();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));

        StepVerifier.create(sut.changeColumnName(command))
            .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.NAME_RESERVED))
            .verify();

        then(changeColumnNamePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("잘못된 형식이면")
    class WithInvalidFormat {

      @Test
      @DisplayName("ColumnNameInvalidException이 발생한다")
      void throwsColumnNameInvalidException() {
        var command = ColumnFixture.changeNameCommand("123invalid");

        StepVerifier.create(sut.changeColumnName(command))
            .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.NAME_INVALID))
            .verify();

        then(changeColumnNamePort).shouldHaveNoInteractions();
      }

    }

  }

}
