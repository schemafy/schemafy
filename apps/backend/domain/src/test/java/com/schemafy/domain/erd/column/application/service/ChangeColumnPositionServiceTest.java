package com.schemafy.domain.erd.column.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.column.application.port.out.ChangeColumnPositionPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnByIdPort;
import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.domain.exception.ColumnNotExistException;
import com.schemafy.domain.erd.column.domain.exception.ColumnPositionInvalidException;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.column.domain.Column;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeColumnPositionService")
class ChangeColumnPositionServiceTest {

  @Mock
  ChangeColumnPositionPort changeColumnPositionPort;

  @Mock
  GetColumnByIdPort getColumnByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @InjectMocks
  ChangeColumnPositionService sut;

  @Nested
  @DisplayName("changeColumnPosition 메서드는")
  class ChangeColumnPosition {

    @Nested
    @DisplayName("유효한 요청이 주어지면")
    class WithValidRequest {

      @Test
      @DisplayName("위치를 변경한다")
      void changesPosition() {
        var command = ColumnFixture.changePositionCommand(1);
        var column = ColumnFixture.defaultColumn();
        var columns = List.of(
            column,
            new Column(
                "01ARZ3NDEKTSV4RRFFQ69G5C02",
                ColumnFixture.DEFAULT_TABLE_ID,
                "col_2",
                "VARCHAR",
                column.lengthScale(),
                1,
                false,
                null,
                null,
                null));

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(columns));
        given(changeColumnPositionPort.changeColumnPositions(any(), anyList()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnPosition(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnPositionPort).should()
            .changeColumnPositions(eq(column.tableId()), anyList());
      }

      @Test
      @DisplayName("같은 위치로 이동해도 정상 처리된다")
      void changesPositionToSameIndex() {
        var command = ColumnFixture.changePositionCommand(0);
        var column = ColumnFixture.defaultColumn();
        var columns = List.of(
            column,
            new Column(
                "01ARZ3NDEKTSV4RRFFQ69G5C02",
                ColumnFixture.DEFAULT_TABLE_ID,
                "col_2",
                "VARCHAR",
                column.lengthScale(),
                1,
                false,
                null,
                null,
                null));

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(columns));
        given(changeColumnPositionPort.changeColumnPositions(any(), anyList()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnPosition(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnPositionPort).should()
            .changeColumnPositions(eq(column.tableId()), anyList());
      }

    }

    @Nested
    @DisplayName("음수 위치가 주어지면")
    class WithNegativePosition {

      @Test
      @DisplayName("첫 번째 위치로 clamp되어 정상 처리된다")
      void clampsToFirstPosition() {
        var command = ColumnFixture.changePositionCommand(-1);
        var column = ColumnFixture.defaultColumn();
        var columns = List.of(
            column,
            new Column(
                "01ARZ3NDEKTSV4RRFFQ69G5C02",
                ColumnFixture.DEFAULT_TABLE_ID,
                "col_2",
                "VARCHAR",
                column.lengthScale(),
                1,
                false,
                null,
                null,
                null));

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(columns));
        given(changeColumnPositionPort.changeColumnPositions(any(), anyList()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnPosition(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnPositionPort).should()
            .changeColumnPositions(eq(column.tableId()), anyList());
      }

    }

    @Nested
    @DisplayName("범위를 벗어난 위치가 주어지면")
    class WithPositionOutOfRange {

      @Test
      @DisplayName("마지막 위치로 clamp되어 정상 처리된다")
      void clampsToLastPosition() {
        var command = ColumnFixture.changePositionCommand(5);
        var column = ColumnFixture.defaultColumn();
        var columns = List.of(
            column,
            new Column(
                "01ARZ3NDEKTSV4RRFFQ69G5C02",
                ColumnFixture.DEFAULT_TABLE_ID,
                "col_2",
                "VARCHAR",
                column.lengthScale(),
                1,
                false,
                null,
                null,
                null));

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(columns));
        given(changeColumnPositionPort.changeColumnPositions(any(), anyList()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnPosition(command))
            .expectNextCount(1)
            .verifyComplete();

        then(changeColumnPositionPort).should()
            .changeColumnPositions(eq(column.tableId()), anyList());
      }

    }

    @Nested
    @DisplayName("컬럼이 존재하지 않으면")
    class WhenColumnNotExists {

      @Test
      @DisplayName("예외가 발생한다")
      void throwsException() {
        var command = ColumnFixture.changePositionCommand(5);

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.empty());

        StepVerifier.create(sut.changeColumnPosition(command))
            .expectError(ColumnNotExistException.class)
            .verify();

        then(getColumnsByTableIdPort).shouldHaveNoInteractions();
        then(changeColumnPositionPort).shouldHaveNoInteractions();
      }

    }

    @Nested
    @DisplayName("테이블 컬럼 목록이 비어 있으면")
    class WhenColumnsEmpty {

      @Test
      @DisplayName("ColumnPositionInvalidException이 발생한다")
      void throwsException() {
        var command = ColumnFixture.changePositionCommand(0);
        var column = ColumnFixture.defaultColumn();

        given(getColumnByIdPort.findColumnById(any()))
            .willReturn(Mono.just(column));
        given(getColumnsByTableIdPort.findColumnsByTableId(any()))
            .willReturn(Mono.just(List.of()));

        StepVerifier.create(sut.changeColumnPosition(command))
            .expectError(ColumnPositionInvalidException.class)
            .verify();

        then(changeColumnPositionPort).shouldHaveNoInteractions();
      }

    }

  }

}
