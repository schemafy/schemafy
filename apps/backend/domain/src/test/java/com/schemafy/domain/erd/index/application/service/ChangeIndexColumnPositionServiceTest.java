package com.schemafy.domain.erd.index.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.index.application.port.out.ChangeIndexColumnPositionPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.domain.exception.IndexPositionInvalidException;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeIndexColumnPositionService")
class ChangeIndexColumnPositionServiceTest {

  @Mock
  ChangeIndexColumnPositionPort changeIndexColumnPositionPort;

  @Mock
  GetIndexColumnByIdPort getIndexColumnByIdPort;

  @Mock
  GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  @InjectMocks
  ChangeIndexColumnPositionService sut;

  @Nested
  @DisplayName("changeIndexColumnPosition 메서드는")
  class ChangeIndexColumnPosition {

    @Test
    @DisplayName("유효한 위치로 컬럼 순서를 변경한다")
    void changesPositionWithValidSeqNo() {
      var command = IndexFixture.changeColumnPositionCommand("ic1", 1);
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC);
      var columns = List.of(
          indexColumn,
          IndexFixture.indexColumn("ic2", "index1", "col2", 1, SortDirection.ASC),
          IndexFixture.indexColumn("ic3", "index1", "col3", 2, SortDirection.DESC));

      given(getIndexColumnByIdPort.findIndexColumnById(any()))
          .willReturn(Mono.just(indexColumn));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(any()))
          .willReturn(Mono.just(columns));
      given(changeIndexColumnPositionPort.changeIndexColumnPositions(any(), anyList()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexColumnPosition(command))
          .verifyComplete();

      then(changeIndexColumnPositionPort).should()
          .changeIndexColumnPositions(eq("index1"), anyList());
    }

    @Test
    @DisplayName("첫 번째 위치(0)로 컬럼을 이동한다")
    void movesColumnToFirstPosition() {
      var command = IndexFixture.changeColumnPositionCommand("ic2", 0);
      var indexColumn = IndexFixture.indexColumn("ic2", "index1", "col2", 1, SortDirection.ASC);
      var columns = List.of(
          IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC),
          indexColumn,
          IndexFixture.indexColumn("ic3", "index1", "col3", 2, SortDirection.DESC));

      given(getIndexColumnByIdPort.findIndexColumnById(any()))
          .willReturn(Mono.just(indexColumn));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(any()))
          .willReturn(Mono.just(columns));
      given(changeIndexColumnPositionPort.changeIndexColumnPositions(any(), anyList()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexColumnPosition(command))
          .verifyComplete();

      then(changeIndexColumnPositionPort).should()
          .changeIndexColumnPositions(eq("index1"), anyList());
    }

    @Test
    @DisplayName("마지막 위치로 컬럼을 이동한다")
    void movesColumnToLastPosition() {
      var command = IndexFixture.changeColumnPositionCommand("ic1", 2);
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC);
      var columns = List.of(
          indexColumn,
          IndexFixture.indexColumn("ic2", "index1", "col2", 1, SortDirection.ASC),
          IndexFixture.indexColumn("ic3", "index1", "col3", 2, SortDirection.DESC));

      given(getIndexColumnByIdPort.findIndexColumnById(any()))
          .willReturn(Mono.just(indexColumn));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(any()))
          .willReturn(Mono.just(columns));
      given(changeIndexColumnPositionPort.changeIndexColumnPositions(any(), anyList()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexColumnPosition(command))
          .verifyComplete();

      then(changeIndexColumnPositionPort).should()
          .changeIndexColumnPositions(eq("index1"), anyList());
    }

    @Test
    @DisplayName("음수 위치면 예외가 발생한다")
    void throwsWhenNegativePosition() {
      var command = IndexFixture.changeColumnPositionCommand("ic1", -1);

      StepVerifier.create(sut.changeIndexColumnPosition(command))
          .expectError(IndexPositionInvalidException.class)
          .verify();

      then(changeIndexColumnPositionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("범위를 벗어난 위치면 예외가 발생한다")
    void throwsWhenPositionOutOfRange() {
      var command = IndexFixture.changeColumnPositionCommand("ic1", 5);
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC);
      var columns = List.of(
          indexColumn,
          IndexFixture.indexColumn("ic2", "index1", "col2", 1, SortDirection.ASC));

      given(getIndexColumnByIdPort.findIndexColumnById(any()))
          .willReturn(Mono.just(indexColumn));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(any()))
          .willReturn(Mono.just(columns));

      StepVerifier.create(sut.changeIndexColumnPosition(command))
          .expectError(IndexPositionInvalidException.class)
          .verify();

      then(changeIndexColumnPositionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인덱스 컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenIndexColumnNotExists() {
      var command = IndexFixture.changeColumnPositionCommand("nonexistent", 0);

      given(getIndexColumnByIdPort.findIndexColumnById(any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexColumnPosition(command))
          .expectError(IndexPositionInvalidException.class)
          .verify();

      then(changeIndexColumnPositionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("컬럼 목록이 비어있으면 예외가 발생한다")
    void throwsWhenColumnsEmpty() {
      var command = IndexFixture.changeColumnPositionCommand("ic1", 0);
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC);

      given(getIndexColumnByIdPort.findIndexColumnById(any()))
          .willReturn(Mono.just(indexColumn));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(any()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.changeIndexColumnPosition(command))
          .expectError(IndexPositionInvalidException.class)
          .verify();

      then(changeIndexColumnPositionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 위치로 이동하면 정상적으로 처리된다")
    void handlesSamePositionMove() {
      var command = IndexFixture.changeColumnPositionCommand("ic1", 0);
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC);
      var columns = List.of(
          indexColumn,
          IndexFixture.indexColumn("ic2", "index1", "col2", 1, SortDirection.ASC));

      given(getIndexColumnByIdPort.findIndexColumnById(any()))
          .willReturn(Mono.just(indexColumn));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId(any()))
          .willReturn(Mono.just(columns));
      given(changeIndexColumnPositionPort.changeIndexColumnPositions(any(), anyList()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexColumnPosition(command))
          .verifyComplete();

      then(changeIndexColumnPositionPort).should()
          .changeIndexColumnPositions(eq("index1"), anyList());
    }

  }

}
