package com.schemafy.domain.erd.column.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.CreateColumnPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.exception.ColumnAutoIncrementNotAllowedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnCharsetNotAllowedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnLengthRequiredException;
import com.schemafy.domain.erd.column.domain.exception.ColumnNameDuplicateException;
import com.schemafy.domain.erd.column.domain.exception.ColumnNameReservedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnPrecisionRequiredException;
import com.schemafy.domain.erd.column.domain.exception.MultipleAutoIncrementColumnException;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.domain.Schema;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateColumnService")
class CreateColumnServiceTest {

  private static final String SCHEMA_ID = "01ARZ3NDEKTSV4RRFFQ69G5SCH";
  private static final String PROJECT_ID = "01ARZ3NDEKTSV4RRFFQ69G5PRJ";

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateColumnPort createColumnPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetSchemaByIdPort getSchemaByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @InjectMocks
  CreateColumnService sut;

  @Nested
  @DisplayName("createColumn 메서드는")
  class CreateColumn {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("VARCHAR 컬럼을 생성한다")
      void createsVarcharColumn() {
        var command = ColumnFixture.createCommand();
        var table = createTable();
        var schema = createSchema();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));
        given(ulidGeneratorPort.generate())
            .willReturn(ColumnFixture.DEFAULT_ID);
        given(createColumnPort.createColumn(any(Column.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sut.createColumn(command))
            .assertNext(result -> {
              assertThat(result.columnId()).isEqualTo(ColumnFixture.DEFAULT_ID);
              assertThat(result.name()).isEqualTo(command.name());
              assertThat(result.dataType()).isEqualTo("VARCHAR");
              assertThat(result.lengthScale().length()).isEqualTo(command.length());
            })
            .verifyComplete();

        then(createColumnPort).should().createColumn(any(Column.class));
      }

      @Test
      @DisplayName("INT 컬럼을 생성한다")
      void createsIntColumn() {
        var command = ColumnFixture.createIntCommand();
        var table = createTable();
        var schema = createSchema();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));
        given(ulidGeneratorPort.generate())
            .willReturn(ColumnFixture.DEFAULT_ID);
        given(createColumnPort.createColumn(any(Column.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sut.createColumn(command))
            .assertNext(result -> {
              assertThat(result.dataType()).isEqualTo("INT");
              assertThat(result.lengthScale()).isNull();
            })
            .verifyComplete();
      }

      @Test
      @DisplayName("DECIMAL 컬럼을 생성한다")
      void createsDecimalColumn() {
        var command = ColumnFixture.createDecimalCommand();
        var table = createTable();
        var schema = createSchema();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));
        given(ulidGeneratorPort.generate())
            .willReturn(ColumnFixture.DEFAULT_ID);
        given(createColumnPort.createColumn(any(Column.class)))
            .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(sut.createColumn(command))
            .assertNext(result -> {
              assertThat(result.dataType()).isEqualTo("DECIMAL");
              assertThat(result.lengthScale().precision()).isEqualTo(10);
              assertThat(result.lengthScale().scale()).isEqualTo(2);
            })
            .verifyComplete();
      }

    }

    @Nested
    @DisplayName("테이블이 존재하지 않으면")
    class WhenTableNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = ColumnFixture.createCommand();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.createColumn(command))
            .expectError(RuntimeException.class)
            .verify();

        then(createColumnPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("스키마가 존재하지 않으면")
    class WhenSchemaNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = ColumnFixture.createCommand();
        var table = createTable();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.createColumn(command))
            .expectError(RuntimeException.class)
            .verify();

        then(createColumnPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("중복된 이름이 존재하면")
    class WithDuplicateName {

      @Test
      @DisplayName("ColumnNameDuplicateException이 발생한다")
      void throwsColumnNameDuplicateException() {
        var command = ColumnFixture.createCommand();
        var table = createTable();
        var schema = createSchema();
        var existingColumn = ColumnFixture.columnWithName(command.name());

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(existingColumn)));

        StepVerifier.create(sut.createColumn(command))
            .expectError(ColumnNameDuplicateException.class)
            .verify();

        then(createColumnPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("예약어 이름이면")
    class WithReservedKeywordName {

      @Test
      @DisplayName("ColumnNameReservedException이 발생한다")
      void throwsColumnNameReservedException() {
        var command = ColumnFixture.createCommandWithName("SELECT");
        var table = createTable();
        var schema = createSchema();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.createColumn(command))
            .expectError(ColumnNameReservedException.class)
            .verify();

        then(createColumnPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("VARCHAR에 length가 없으면")
    class WhenVarcharWithoutLength {

      @Test
      @DisplayName("ColumnLengthRequiredException이 발생한다")
      void throwsColumnLengthRequiredException() {
        var command = ColumnFixture.createCommandWithDataType("VARCHAR", null, null, null);
        var table = createTable();
        var schema = createSchema();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.createColumn(command))
            .expectError(ColumnLengthRequiredException.class)
            .verify();

        then(createColumnPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("DECIMAL에 precision이 없으면")
    class WhenDecimalWithoutPrecision {

      @Test
      @DisplayName("ColumnPrecisionRequiredException이 발생한다")
      void throwsColumnPrecisionRequiredException() {
        var command = ColumnFixture.createCommandWithDataType("DECIMAL", null, null, null);
        var table = createTable();
        var schema = createSchema();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.createColumn(command))
            .expectError(ColumnPrecisionRequiredException.class)
            .verify();

        then(createColumnPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("비정수 타입에 autoIncrement를 설정하면")
    class WhenNonIntegerWithAutoIncrement {

      @Test
      @DisplayName("ColumnAutoIncrementNotAllowedException이 발생한다")
      void throwsColumnAutoIncrementNotAllowedException() {
        var command = ColumnFixture.createCommandWithAutoIncrement("BIGINT");
        var commandWithWrongType = new com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand(
            command.tableId(),
            command.name(),
            "TEXT",
            null,
            null,
            null,
            command.seqNo(),
            true,
            null,
            null,
            null);
        var table = createTable();
        var schema = createSchema();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.createColumn(commandWithWrongType))
            .expectError(ColumnAutoIncrementNotAllowedException.class)
            .verify();

        then(createColumnPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("이미 autoIncrement 컬럼이 있으면")
    class WhenAutoIncrementAlreadyExists {

      @Test
      @DisplayName("MultipleAutoIncrementColumnException이 발생한다")
      void throwsMultipleAutoIncrementColumnException() {
        var command = ColumnFixture.createCommandWithAutoIncrement("INT");
        var table = createTable();
        var schema = createSchema();
        var existingAutoIncrement = ColumnFixture.intColumnWithAutoIncrementAndName(
            "01ARZ3NDEKTSV4RRFFQ69G5EXS", "existing_auto_increment");

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(existingAutoIncrement)));

        StepVerifier.create(sut.createColumn(command))
            .expectError(MultipleAutoIncrementColumnException.class)
            .verify();

        then(createColumnPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("INT에 charset을 설정하면")
    class WhenIntWithCharset {

      @Test
      @DisplayName("ColumnCharsetNotAllowedException이 발생한다")
      void throwsColumnCharsetNotAllowedException() {
        var command = ColumnFixture.createCommandWithCharset("utf8mb4", "utf8mb4_general_ci");
        var commandWithInt = new com.schemafy.domain.erd.column.application.port.in.CreateColumnCommand(
            command.tableId(),
            command.name(),
            "INT",
            null,
            null,
            null,
            command.seqNo(),
            false,
            "utf8mb4",
            "utf8mb4_general_ci",
            null);
        var table = createTable();
        var schema = createSchema();

        given(getTableByIdPort.findTableById(any()))
            .willReturn(Mono.just(table));
        given(getSchemaByIdPort.findSchemaById(any()))
            .willReturn(Mono.just(schema));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.createColumn(commandWithInt))
            .expectError(ColumnCharsetNotAllowedException.class)
            .verify();

        then(createColumnPort).shouldHaveNoInteractions();
      }

    }

  }

  private Table createTable() {
    return new Table(
        ColumnFixture.DEFAULT_TABLE_ID,
        SCHEMA_ID,
        "test_table",
        "utf8mb4",
        "utf8mb4_general_ci");
  }

  private Schema createSchema() {
    return new Schema(
        SCHEMA_ID,
        PROJECT_ID,
        "MySQL",
        "test_schema",
        "utf8mb4",
        "utf8mb4_general_ci");
  }

}
