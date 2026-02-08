package com.schemafy.domain.erd.index.application.service;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.domain.erd.index.application.port.out.ChangeIndexColumnSortDirectionPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnSortDirectionInvalidException;
import com.schemafy.domain.erd.index.domain.exception.IndexDefinitionDuplicateException;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;
import com.schemafy.domain.erd.index.domain.type.IndexType;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.index.fixture.IndexFixture;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeIndexColumnSortDirectionService")
class ChangeIndexColumnSortDirectionServiceTest {

  @Mock
  ChangeIndexColumnSortDirectionPort changeIndexColumnSortDirectionPort;

  @Mock
  GetIndexColumnByIdPort getIndexColumnByIdPort;

  @Mock
  GetIndexByIdPort getIndexByIdPort;

  @Mock
  GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  @Mock
  GetIndexesByTableIdPort getIndexesByTableIdPort;

  @InjectMocks
  ChangeIndexColumnSortDirectionService sut;

  @Nested
  @DisplayName("changeIndexColumnSortDirection 메서드는")
  class ChangeIndexColumnSortDirection {

    @Test
    @DisplayName("유효한 요청에 대해 정렬 방향을 변경한다")
    void changesSortDirectionWithValidRequest() {
      var command = IndexFixture.changeSortDirectionCommand("ic1", SortDirection.DESC);
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC);
      var index = IndexFixture.index("index1", "table1", "idx_test", IndexType.BTREE);
      var columns = List.of(indexColumn);

      given(getIndexColumnByIdPort.findIndexColumnById("ic1"))
          .willReturn(Mono.just(indexColumn));
      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(columns));
      given(getIndexesByTableIdPort.findIndexesByTableId("table1"))
          .willReturn(Mono.just(List.of(index)));
      given(changeIndexColumnSortDirectionPort.changeIndexColumnSortDirection(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexColumnSortDirection(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeIndexColumnSortDirectionPort).should()
          .changeIndexColumnSortDirection(eq("ic1"), eq(SortDirection.DESC));
    }

    @Test
    @DisplayName("ASC에서 DESC로 정렬 방향을 변경한다")
    void changesFromAscToDesc() {
      var command = IndexFixture.changeSortDirectionCommand("ic1", SortDirection.DESC);
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC);
      var index = IndexFixture.index("index1", "table1", "idx_test", IndexType.BTREE);
      var columns = List.of(indexColumn);

      given(getIndexColumnByIdPort.findIndexColumnById("ic1"))
          .willReturn(Mono.just(indexColumn));
      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(columns));
      given(getIndexesByTableIdPort.findIndexesByTableId("table1"))
          .willReturn(Mono.just(List.of(index)));
      given(changeIndexColumnSortDirectionPort.changeIndexColumnSortDirection(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexColumnSortDirection(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeIndexColumnSortDirectionPort).should()
          .changeIndexColumnSortDirection(eq("ic1"), eq(SortDirection.DESC));
    }

    @Test
    @DisplayName("DESC에서 ASC로 정렬 방향을 변경한다")
    void changesFromDescToAsc() {
      var command = IndexFixture.changeSortDirectionCommand("ic1", SortDirection.ASC);
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.DESC);
      var index = IndexFixture.index("index1", "table1", "idx_test", IndexType.BTREE);
      var columns = List.of(indexColumn);

      given(getIndexColumnByIdPort.findIndexColumnById("ic1"))
          .willReturn(Mono.just(indexColumn));
      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(columns));
      given(getIndexesByTableIdPort.findIndexesByTableId("table1"))
          .willReturn(Mono.just(List.of(index)));
      given(changeIndexColumnSortDirectionPort.changeIndexColumnSortDirection(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexColumnSortDirection(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeIndexColumnSortDirectionPort).should()
          .changeIndexColumnSortDirection(eq("ic1"), eq(SortDirection.ASC));
    }

    @Test
    @DisplayName("정렬 방향이 null이면 예외가 발생한다")
    void throwsWhenSortDirectionIsNull() {
      var command = IndexFixture.changeSortDirectionCommand("ic1", null);

      StepVerifier.create(sut.changeIndexColumnSortDirection(command))
          .expectError(IndexColumnSortDirectionInvalidException.class)
          .verify();

      then(changeIndexColumnSortDirectionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인덱스 컬럼이 존재하지 않으면 예외가 발생한다")
    void throwsWhenIndexColumnNotExists() {
      var command = IndexFixture.changeSortDirectionCommand("nonexistent", SortDirection.ASC);

      given(getIndexColumnByIdPort.findIndexColumnById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexColumnSortDirection(command))
          .expectError(IndexColumnSortDirectionInvalidException.class)
          .verify();

      then(changeIndexColumnSortDirectionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("인덱스가 존재하지 않으면 예외가 발생한다")
    void throwsWhenIndexNotExists() {
      var command = IndexFixture.changeSortDirectionCommand("ic1", SortDirection.ASC);
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.DESC);

      given(getIndexColumnByIdPort.findIndexColumnById("ic1"))
          .willReturn(Mono.just(indexColumn));
      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexColumnSortDirection(command))
          .expectError(IndexNotExistException.class)
          .verify();

      then(changeIndexColumnSortDirectionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("변경 후 중복 인덱스 정의가 되면 예외가 발생한다")
    void throwsWhenDuplicateDefinitionAfterChange() {
      var command = IndexFixture.changeSortDirectionCommand("ic1", SortDirection.DESC);
      var indexColumn = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC);
      var index1 = IndexFixture.index("index1", "table1", "idx_test1", IndexType.BTREE);
      var index2 = IndexFixture.index("index2", "table1", "idx_test2", IndexType.BTREE);
      var columns1 = List.of(indexColumn);
      var columns2 = List.of(
          IndexFixture.indexColumn("ic2", "index2", "col1", 0, SortDirection.DESC));

      given(getIndexColumnByIdPort.findIndexColumnById("ic1"))
          .willReturn(Mono.just(indexColumn));
      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index1));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(columns1));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index2"))
          .willReturn(Mono.just(columns2));
      given(getIndexesByTableIdPort.findIndexesByTableId("table1"))
          .willReturn(Mono.just(List.of(index1, index2)));

      StepVerifier.create(sut.changeIndexColumnSortDirection(command))
          .expectError(IndexDefinitionDuplicateException.class)
          .verify();

      then(changeIndexColumnSortDirectionPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다중 컬럼 인덱스에서 정렬 방향을 변경한다")
    void changesSortDirectionInMultiColumnIndex() {
      var command = IndexFixture.changeSortDirectionCommand("ic1", SortDirection.DESC);
      var indexColumn1 = IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC);
      var indexColumn2 = IndexFixture.indexColumn("ic2", "index1", "col2", 1, SortDirection.ASC);
      var index = IndexFixture.index("index1", "table1", "idx_test", IndexType.BTREE);
      var columns = List.of(indexColumn1, indexColumn2);

      given(getIndexColumnByIdPort.findIndexColumnById("ic1"))
          .willReturn(Mono.just(indexColumn1));
      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(columns));
      given(getIndexesByTableIdPort.findIndexesByTableId("table1"))
          .willReturn(Mono.just(List.of(index)));
      given(changeIndexColumnSortDirectionPort.changeIndexColumnSortDirection(any(), any()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.changeIndexColumnSortDirection(command))
          .expectNextCount(1)
          .verifyComplete();

      then(changeIndexColumnSortDirectionPort).should()
          .changeIndexColumnSortDirection(eq("ic1"), eq(SortDirection.DESC));
    }

  }

}
