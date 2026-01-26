package com.schemafy.domain.erd.column.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.ChangeColumnMetaPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.exception.ColumnAutoIncrementNotAllowedException;
import com.schemafy.domain.erd.column.domain.exception.ColumnCharsetNotAllowedException;
import com.schemafy.domain.erd.column.domain.exception.MultipleAutoIncrementColumnException;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeColumnMetaService")
class ChangeColumnMetaServiceTest {

  @Mock
  ChangeColumnMetaPort changeColumnMetaPort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @InjectMocks
  ChangeColumnMetaService sut;

  @Nested
  @DisplayName("changeColumnMeta 메서드는")
  class ChangeColumnMeta {

    @Nested
    @DisplayName("부분 업데이트 시")
    class WithPartialUpdate {

      @Test
      @DisplayName("autoIncrement만 변경한다")
      void changesOnlyAutoIncrement() {
        var command = ColumnFixture.changeMetaCommand(true, null, null, null);
        var column = ColumnFixture.intColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnMeta(command))
            .verifyComplete();

        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(command.columnId()), eq(true), any(), any(), any());
      }

      @Test
      @DisplayName("charset만 변경한다")
      void changesOnlyCharset() {
        var command = ColumnFixture.changeMetaCommand(null, "utf8mb4", null, null);
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnMeta(command))
            .verifyComplete();

        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(command.columnId()), eq(false), eq("utf8mb4"), any(), any());
      }

      @Test
      @DisplayName("comment만 변경한다")
      void changesOnlyComment() {
        var command = ColumnFixture.changeMetaCommand(null, null, null, "New comment");
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));
        given(changeColumnMetaPort.changeColumnMeta(any(), any(), any(), any(), any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnMeta(command))
            .verifyComplete();

        then(changeColumnMetaPort).should()
            .changeColumnMeta(eq(command.columnId()), eq(false), any(), any(), eq("New comment"));
      }

    }

    @Nested
    @DisplayName("컬럼이 존재하지 않으면")
    class WhenColumnNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = ColumnFixture.changeMetaCommand(true, null, null, null);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnMeta(command))
            .expectError(RuntimeException.class)
            .verify();

        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("비정수 컬럼에 autoIncrement를 설정하면")
    class WhenAutoIncrementOnNonIntegerColumn {

      @Test
      @DisplayName("ColumnAutoIncrementNotAllowedException이 발생한다")
      void throwsColumnAutoIncrementNotAllowedException() {
        var command = ColumnFixture.changeMetaCommand(true, null, null, null);
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));

        StepVerifier.create(sut.changeColumnMeta(command))
            .expectError(ColumnAutoIncrementNotAllowedException.class)
            .verify();

        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("INT 컬럼에 charset을 설정하면")
    class WhenCharsetOnIntColumn {

      @Test
      @DisplayName("ColumnCharsetNotAllowedException이 발생한다")
      void throwsColumnCharsetNotAllowedException() {
        var command = ColumnFixture.changeMetaCommand(null, "utf8mb4", null, null);
        var column = ColumnFixture.intColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of(column)));

        StepVerifier.create(sut.changeColumnMeta(command))
            .expectError(ColumnCharsetNotAllowedException.class)
            .verify();

        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("이미 autoIncrement 컬럼이 있으면")
    class WhenAutoIncrementAlreadyExists {

      @Test
      @DisplayName("MultipleAutoIncrementColumnException이 발생한다")
      void throwsMultipleAutoIncrementColumnException() {
        var command = ColumnFixture.changeMetaCommand(true, null, null, null);
        var column = ColumnFixture.intColumn();
        var existingAutoIncrement = ColumnFixture.intColumnWithAutoIncrementAndName(
            "01ARZ3NDEKTSV4RRFFQ69G5EXS", "existing_auto_increment");
        var columns = List.of(column, existingAutoIncrement);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(columns));

        StepVerifier.create(sut.changeColumnMeta(command))
            .expectError(MultipleAutoIncrementColumnException.class)
            .verify();

        then(changeColumnMetaPort).shouldHaveNoInteractions();
      }

    }

  }

}
