package com.schemafy.domain.erd.index.application.service;

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
import com.schemafy.domain.erd.index.application.port.out.CreateIndexColumnPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexByIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnDuplicateException;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnNotExistException;
import com.schemafy.domain.erd.index.domain.exception.IndexNotExistException;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.index.fixture.IndexFixture;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddIndexColumnService")
class AddIndexColumnServiceTest {

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateIndexColumnPort createIndexColumnPort;

  @Mock
  GetIndexByIdPort getIndexByIdPort;

  @Mock
  GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  @Mock
  GetIndexesByTableIdPort getIndexesByTableIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @InjectMocks
  AddIndexColumnService sut;

  @Nested
  @DisplayName("addIndexColumn 메서드는")
  class AddIndexColumn {

    @Test
    @DisplayName("유효한 요청에 대해 컬럼을 추가한다")
    void addsColumnWithValidRequest() {
      var command = IndexFixture.addColumnCommand("index1", "col2", 1, SortDirection.ASC);
      var index = IndexFixture.indexWithId("index1");
      var existingColumns = List.of(
          IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC));
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(existingColumns));
      given(getColumnsByTableIdPort.findColumnsByTableId(index.tableId()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(index.tableId()))
          .willReturn(Mono.just(List.of(index)));
      given(ulidGeneratorPort.generate())
          .willReturn("new-column-id");
      given(createIndexColumnPort.createIndexColumn(any(IndexColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.addIndexColumn(command))
          .assertNext(result -> {
            var payload = result.result();
            assertThat(payload.indexColumnId()).isEqualTo("new-column-id");
            assertThat(payload.indexId()).isEqualTo("index1");
            assertThat(payload.columnId()).isEqualTo("col2");
            assertThat(payload.seqNo()).isEqualTo(1);
            assertThat(payload.sortDirection()).isEqualTo(SortDirection.ASC);
          })
          .verifyComplete();

      then(createIndexColumnPort).should().createIndexColumn(any(IndexColumn.class));
    }

    @Test
    @DisplayName("인덱스가 존재하지 않으면 예외가 발생한다")
    void throwsWhenIndexNotExists() {
      var command = IndexFixture.addColumnCommand("nonexistent", "col1", 0, SortDirection.ASC);

      given(getIndexByIdPort.findIndexById("nonexistent"))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.addIndexColumn(command))
          .expectError(IndexNotExistException.class)
          .verify();

      then(createIndexColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("컬럼이 테이블에 존재하지 않으면 예외가 발생한다")
    void throwsWhenColumnNotExistsInTable() {
      var command = IndexFixture.addColumnCommand("index1", "nonexistent", 1, SortDirection.ASC);
      var index = IndexFixture.indexWithId("index1");
      var existingColumns = List.of(
          IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC));
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(existingColumns));
      given(getColumnsByTableIdPort.findColumnsByTableId(index.tableId()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(index.tableId()))
          .willReturn(Mono.just(List.of(index)));

      StepVerifier.create(sut.addIndexColumn(command))
          .expectError(IndexColumnNotExistException.class)
          .verify();

      then(createIndexColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("중복 컬럼 추가 시 예외가 발생한다")
    void throwsWhenDuplicateColumn() {
      var command = IndexFixture.addColumnCommand("index1", "col1", 1, SortDirection.ASC);
      var index = IndexFixture.indexWithId("index1");
      var existingColumns = List.of(
          IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC));
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(existingColumns));
      given(getColumnsByTableIdPort.findColumnsByTableId(index.tableId()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(index.tableId()))
          .willReturn(Mono.just(List.of(index)));

      StepVerifier.create(sut.addIndexColumn(command))
          .expectError(IndexColumnDuplicateException.class)
          .verify();

      then(createIndexColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DESC 정렬 방향으로 컬럼을 추가한다")
    void addsColumnWithDescSortDirection() {
      var command = IndexFixture.addColumnCommand("index1", "col2", 1, SortDirection.DESC);
      var index = IndexFixture.indexWithId("index1");
      var existingColumns = List.of(
          IndexFixture.indexColumn("ic1", "index1", "col1", 0, SortDirection.ASC));
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));

      given(getIndexByIdPort.findIndexById("index1"))
          .willReturn(Mono.just(index));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("index1"))
          .willReturn(Mono.just(existingColumns));
      given(getColumnsByTableIdPort.findColumnsByTableId(index.tableId()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(index.tableId()))
          .willReturn(Mono.just(List.of(index)));
      given(ulidGeneratorPort.generate())
          .willReturn("new-column-id");
      given(createIndexColumnPort.createIndexColumn(any(IndexColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.addIndexColumn(command))
          .assertNext(result -> {
            assertThat(result.result().sortDirection()).isEqualTo(SortDirection.DESC);
          })
          .verifyComplete();
    }

  }

}
