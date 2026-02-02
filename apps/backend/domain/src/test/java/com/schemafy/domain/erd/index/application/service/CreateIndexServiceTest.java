package com.schemafy.domain.erd.index.application.service;

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

import com.schemafy.domain.erd.column.application.port.out.GetColumnsByTableIdPort;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;
import com.schemafy.domain.erd.index.application.port.out.CreateIndexColumnPort;
import com.schemafy.domain.erd.index.application.port.out.CreateIndexPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexColumnsByIndexIdPort;
import com.schemafy.domain.erd.index.application.port.out.GetIndexesByTableIdPort;
import com.schemafy.domain.erd.index.application.port.out.IndexExistsPort;
import com.schemafy.domain.erd.index.domain.Index;
import com.schemafy.domain.erd.index.domain.IndexColumn;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnDuplicateException;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnNotExistException;
import com.schemafy.domain.erd.index.domain.exception.IndexColumnSortDirectionInvalidException;
import com.schemafy.domain.erd.index.domain.exception.IndexDefinitionDuplicateException;
import com.schemafy.domain.erd.index.domain.exception.IndexNameDuplicateException;
import com.schemafy.domain.erd.index.domain.exception.IndexNameInvalidException;
import com.schemafy.domain.erd.index.domain.exception.IndexPositionInvalidException;
import com.schemafy.domain.erd.index.domain.exception.IndexTypeInvalidException;
import com.schemafy.domain.erd.index.domain.type.IndexType;
import com.schemafy.domain.erd.index.domain.type.SortDirection;
import com.schemafy.domain.erd.index.fixture.IndexFixture;
import com.schemafy.domain.erd.table.application.port.out.GetTableByIdPort;
import com.schemafy.domain.erd.table.domain.Table;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;
import com.schemafy.domain.ulid.application.port.out.UlidGeneratorPort;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateIndexService")
class CreateIndexServiceTest {

  @Mock
  UlidGeneratorPort ulidGeneratorPort;

  @Mock
  CreateIndexPort createIndexPort;

  @Mock
  CreateIndexColumnPort createIndexColumnPort;

  @Mock
  IndexExistsPort indexExistsPort;

  @Mock
  GetTableByIdPort getTableByIdPort;

  @Mock
  GetColumnsByTableIdPort getColumnsByTableIdPort;

  @Mock
  GetIndexesByTableIdPort getIndexesByTableIdPort;

  @Mock
  GetIndexColumnsByIndexIdPort getIndexColumnsByIndexIdPort;

  @Mock
  TransactionalOperator transactionalOperator;

  @InjectMocks
  CreateIndexService sut;

  @BeforeEach
  void setUpTransaction() {
    given(transactionalOperator.transactional(any(Mono.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("createIndex 메서드는")
  class CreateIndex {

    @Test
    @DisplayName("유효한 BTREE 인덱스를 생성한다")
    void createsBtreeIndex() {
      var command = IndexFixture.createBtreeCommandWithColumns(List.of(
          IndexFixture.createAscColumnCommand("col1", 0)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_btree"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate())
          .willReturn("new-index-id", "new-column-id");
      given(createIndexPort.createIndex(any(Index.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createIndexColumnPort.createIndexColumn(any(IndexColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createIndex(command))
          .assertNext(result -> {
            assertThat(result.indexId()).isEqualTo("new-index-id");
            assertThat(result.name()).isEqualTo("idx_btree");
            assertThat(result.type()).isEqualTo(IndexType.BTREE);
          })
          .verifyComplete();

      then(createIndexPort).should().createIndex(any(Index.class));
      then(createIndexColumnPort).should().createIndexColumn(any(IndexColumn.class));
    }

    @Test
    @DisplayName("유효한 HASH 인덱스를 생성한다")
    void createsHashIndex() {
      var command = IndexFixture.createHashCommand();
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(IndexFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_hash"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate())
          .willReturn("new-index-id", "new-column-id");
      given(createIndexPort.createIndex(any(Index.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createIndexColumnPort.createIndexColumn(any(IndexColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createIndex(command))
          .assertNext(result -> {
            assertThat(result.type()).isEqualTo(IndexType.HASH);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("유효한 FULLTEXT 인덱스를 생성한다")
    void createsFulltextIndex() {
      var command = IndexFixture.createFulltextCommand();
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(IndexFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_fulltext"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate())
          .willReturn("new-index-id", "new-column-id");
      given(createIndexPort.createIndex(any(Index.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createIndexColumnPort.createIndexColumn(any(IndexColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createIndex(command))
          .assertNext(result -> {
            assertThat(result.type()).isEqualTo(IndexType.FULLTEXT);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("유효한 SPATIAL 인덱스를 생성한다")
    void createsSpatialIndex() {
      var command = IndexFixture.createSpatialCommand();
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(IndexFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_spatial"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate())
          .willReturn("new-index-id", "new-column-id");
      given(createIndexPort.createIndex(any(Index.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createIndexColumnPort.createIndexColumn(any(IndexColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createIndex(command))
          .assertNext(result -> {
            assertThat(result.type()).isEqualTo(IndexType.SPATIAL);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다중 컬럼 인덱스를 생성한다")
    void createsMultiColumnIndex() {
      var command = IndexFixture.createBtreeCommandWithColumns(List.of(
          IndexFixture.createAscColumnCommand("col1", 0),
          IndexFixture.createDescColumnCommand("col2", 1)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_btree"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate())
          .willReturn("new-index-id", "ic1", "ic2");
      given(createIndexPort.createIndex(any(Index.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createIndexColumnPort.createIndexColumn(any(IndexColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createIndex(command))
          .assertNext(result -> {
            assertThat(result.indexId()).isEqualTo("new-index-id");
            assertThat(result.name()).isEqualTo("idx_btree");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("이름이 null이면 예외가 발생한다")
    void throwsWhenNameIsNull() {
      var command = IndexFixture.createCommandWithName(null);

      StepVerifier.create(sut.createIndex(command))
          .expectError(IndexNameInvalidException.class)
          .verify();

      then(createIndexPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이름이 빈 문자열이면 예외가 발생한다")
    void throwsWhenNameIsEmpty() {
      var command = IndexFixture.createCommandWithName("  ");

      StepVerifier.create(sut.createIndex(command))
          .expectError(IndexNameInvalidException.class)
          .verify();

      then(createIndexPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("타입이 null이면 예외가 발생한다")
    void throwsWhenTypeIsNull() {
      var command = IndexFixture.createCommandWithType(null);

      StepVerifier.create(sut.createIndex(command))
          .expectError(IndexTypeInvalidException.class)
          .verify();

      then(createIndexPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("테이블이 존재하지 않으면 예외가 발생한다")
    void throwsWhenTableNotExists() {
      var command = IndexFixture.createBtreeCommand();

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.empty());

      StepVerifier.create(sut.createIndex(command))
          .expectError(TableNotExistException.class)
          .verify();

      then(createIndexPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("중복된 이름이면 예외가 발생한다")
    void throwsWhenNameIsDuplicate() {
      var command = IndexFixture.createBtreeCommand();
      var table = createTable("table1", "schema1");

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_btree"))
          .willReturn(Mono.just(true));

      StepVerifier.create(sut.createIndex(command))
          .expectError(IndexNameDuplicateException.class)
          .verify();

      then(createIndexPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("컬럼이 테이블에 존재하지 않으면 예외가 발생한다")
    void throwsWhenColumnNotExistsInTable() {
      var command = IndexFixture.createBtreeCommandWithColumns(List.of(
          IndexFixture.createAscColumnCommand("nonexistent", 0)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_btree"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createIndex(command))
          .expectError(IndexColumnNotExistException.class)
          .verify();

      then(createIndexPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("중복 컬럼이 포함되면 예외가 발생한다")
    void throwsWhenDuplicateColumns() {
      var command = IndexFixture.createBtreeCommandWithColumns(List.of(
          IndexFixture.createAscColumnCommand("col1", 0),
          IndexFixture.createAscColumnCommand("col1", 1)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_btree"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createIndex(command))
          .expectError(IndexColumnDuplicateException.class)
          .verify();

      then(createIndexPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("seqNo가 연속적이지 않으면 예외가 발생한다")
    void throwsWhenSeqNoNotSequential() {
      var command = IndexFixture.createBtreeCommandWithColumns(List.of(
          IndexFixture.createAscColumnCommand("col1", 0),
          IndexFixture.createAscColumnCommand("col2", 2)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(
          ColumnFixture.columnWithId("col1"),
          ColumnFixture.columnWithId("col2"));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_btree"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createIndex(command))
          .expectError(IndexPositionInvalidException.class)
          .verify();

      then(createIndexPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("정렬 방향이 null이면 예외가 발생한다")
    void throwsWhenSortDirectionIsNull() {
      var command = IndexFixture.createBtreeCommandWithColumns(List.of(
          IndexFixture.createColumnCommand("col1", 0, null)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_btree"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of()));

      StepVerifier.create(sut.createIndex(command))
          .expectError(IndexColumnSortDirectionInvalidException.class)
          .verify();

      then(createIndexPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 컬럼 조합과 타입의 인덱스가 이미 존재하면 예외가 발생한다")
    void throwsWhenDefinitionAlreadyExists() {
      var command = IndexFixture.createBtreeCommandWithColumns(List.of(
          IndexFixture.createAscColumnCommand("col1", 0)));
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId("col1"));
      var existingIndex = IndexFixture.index("existing-idx", table.id(), "existing_idx", IndexType.BTREE);
      var existingColumns = List.of(
          IndexFixture.indexColumn("ic1", "existing-idx", "col1", 0, SortDirection.ASC));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_btree"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of(existingIndex)));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("existing-idx"))
          .willReturn(Mono.just(existingColumns));

      StepVerifier.create(sut.createIndex(command))
          .expectError(IndexDefinitionDuplicateException.class)
          .verify();

      then(createIndexPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("다른 타입이면 같은 컬럼 조합도 생성 가능하다")
    void allowsSameColumnsWithDifferentType() {
      var command = IndexFixture.createHashCommand();
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(IndexFixture.DEFAULT_COLUMN_ID));
      var existingIndex = IndexFixture.index("existing-idx", table.id(), "existing_idx", IndexType.BTREE);
      var existingColumns = List.of(
          IndexFixture.indexColumn("ic1", "existing-idx", IndexFixture.DEFAULT_COLUMN_ID, 0, SortDirection.ASC));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_hash"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of(existingIndex)));
      given(getIndexColumnsByIndexIdPort.findIndexColumnsByIndexId("existing-idx"))
          .willReturn(Mono.just(existingColumns));
      given(ulidGeneratorPort.generate())
          .willReturn("new-index-id", "new-column-id");
      given(createIndexPort.createIndex(any(Index.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createIndexColumnPort.createIndexColumn(any(IndexColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createIndex(command))
          .assertNext(result -> {
            assertThat(result.type()).isEqualTo(IndexType.HASH);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("컬럼이 없으면 컬럼 생성 없이 인덱스만 생성한다")
    void createsIndexWithoutColumnsWhenEmpty() {
      var command = IndexFixture.createCommandWithColumns(List.of());
      var table = createTable("table1", "schema1");

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), command.name()))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate())
          .willReturn("new-index-id");
      given(createIndexPort.createIndex(any(Index.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createIndex(command))
          .assertNext(result -> assertThat(result.indexId()).isEqualTo("new-index-id"))
          .verifyComplete();

      then(createIndexColumnPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이름 앞뒤 공백을 제거하고 생성한다")
    void trimsNameBeforeCreating() {
      var command = IndexFixture.createCommandWithName("  idx_test  ");
      var table = createTable("table1", "schema1");
      var tableColumns = List.of(ColumnFixture.columnWithId(IndexFixture.DEFAULT_COLUMN_ID));

      given(getTableByIdPort.findTableById(command.tableId()))
          .willReturn(Mono.just(table));
      given(indexExistsPort.existsByTableIdAndName(table.id(), "idx_test"))
          .willReturn(Mono.just(false));
      given(getColumnsByTableIdPort.findColumnsByTableId(table.id()))
          .willReturn(Mono.just(tableColumns));
      given(getIndexesByTableIdPort.findIndexesByTableId(table.id()))
          .willReturn(Mono.just(List.of()));
      given(ulidGeneratorPort.generate())
          .willReturn("new-index-id", "new-column-id");
      given(createIndexPort.createIndex(any(Index.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));
      given(createIndexColumnPort.createIndexColumn(any(IndexColumn.class)))
          .willAnswer(invocation -> Mono.just(invocation.getArgument(0)));

      StepVerifier.create(sut.createIndex(command))
          .assertNext(result -> {
            assertThat(result.name()).isEqualTo("idx_test");
          })
          .verifyComplete();
    }

  }

  private Table createTable(String id, String schemaId) {
    return new Table(id, schemaId, "test_table", null, null);
  }

}
