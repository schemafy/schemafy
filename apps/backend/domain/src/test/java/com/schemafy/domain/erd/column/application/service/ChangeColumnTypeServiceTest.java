package com.schemafy.domain.erd.column.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.ChangeColumnTypePort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;
import com.schemafy.domain.erd.column.domain.exception.ColumnAutoIncrementNotAllowedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnCharsetNotAllowedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnPrecisionRequiredException;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeColumnTypeService")
class ChangeColumnTypeServiceTest {

  @Mock
  ChangeColumnTypePort changeColumnTypePort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @InjectMocks
  ChangeColumnTypeService sut;

  @Nested
  @DisplayName("changeColumnType 메서드는")
  class ChangeColumnType {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("INT에서 BIGINT로 변경한다")
      void changesIntToBigint() {
        var command = ColumnFixture.changeTypeCommand("BIGINT", null, null, null);
        var column = ColumnFixture.intColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnType(command))
            .verifyComplete();

        then(changeColumnTypePort).should()
            .changeColumnType(eq(command.columnId()), eq("BIGINT"), any());
      }

      @Test
      @DisplayName("VARCHAR에서 TEXT로 변경한다")
      void changesVarcharToText() {
        var command = ColumnFixture.changeTypeCommand("TEXT", null, null, null);
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnTypePort.changeColumnType(any(), any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnType(command))
            .verifyComplete();

        then(changeColumnTypePort).should()
            .changeColumnType(eq(command.columnId()), eq("TEXT"), any());
      }

    }

    @Nested
    @DisplayName("컬럼이 존재하지 않으면")
    class WhenColumnNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = ColumnFixture.changeTypeCommand("BIGINT", null, null, null);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnType(command))
            .expectError(RuntimeException.class)
            .verify();

        then(changeColumnTypePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("INT에서 DECIMAL로 변경할 때 precision이 없으면")
    class WhenIntToDecimalWithoutPrecision {

      @Test
      @DisplayName("ColumnPrecisionRequiredException이 발생한다")
      void throwsColumnPrecisionRequiredException() {
        var command = ColumnFixture.changeTypeCommand("DECIMAL", null, null, null);
        var column = ColumnFixture.intColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));

        StepVerifier.create(sut.changeColumnType(command))
            .expectError(ColumnPrecisionRequiredException.class)
            .verify();

        then(changeColumnTypePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("autoIncrement 컬럼을 VARCHAR로 변경하면")
    class WhenAutoIncrementToVarchar {

      @Test
      @DisplayName("ColumnAutoIncrementNotAllowedException이 발생한다")
      void throwsColumnAutoIncrementNotAllowedException() {
        var command = ColumnFixture.changeTypeCommand("VARCHAR", 255, null, null);
        var column = ColumnFixture.intColumnWithAutoIncrement();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));

        StepVerifier.create(sut.changeColumnType(command))
            .expectError(ColumnAutoIncrementNotAllowedException.class)
            .verify();

        then(changeColumnTypePort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("charset이 있는 컬럼을 INT로 변경하면")
    class WhenCharsetColumnToInt {

      @Test
      @DisplayName("ColumnCharsetNotAllowedException이 발생한다")
      void throwsColumnCharsetNotAllowedException() {
        var command = ColumnFixture.changeTypeCommand("INT", null, null, null);
        var column = new Column(
            ColumnFixture.DEFAULT_ID,
            ColumnFixture.DEFAULT_TABLE_ID,
            ColumnFixture.DEFAULT_NAME,
            "VARCHAR",
            new ColumnLengthScale(255, null, null),
            ColumnFixture.DEFAULT_SEQ_NO,
            false,
            "utf8mb4",
            "utf8mb4_general_ci",
            null);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));

        StepVerifier.create(sut.changeColumnType(command))
            .expectError(ColumnCharsetNotAllowedException.class)
            .verify();

        then(changeColumnTypePort).shouldHaveNoInteractions();
      }

    }

  }

}
