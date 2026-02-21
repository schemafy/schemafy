package com.schemafy.domain.erd.column.adapter.out.persistence;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.config.R2dbcTestConfiguration;
import com.schemafy.domain.erd.column.domain.Column;
import com.schemafy.domain.erd.column.domain.ColumnLengthScale;
import com.schemafy.domain.erd.column.domain.exception.ColumnErrorCode;
import com.schemafy.domain.erd.column.fixture.ColumnFixture;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({ ColumnPersistenceAdapter.class, ColumnMapper.class, R2dbcTestConfiguration.class })
@DisplayName("ColumnPersistenceAdapter")
class ColumnPersistenceAdapterTest {

  private static final String OTHER_COLUMN_ID = "01ARZ3NDEKTSV4RRFFQ69G5OTH";
  private static final String OTHER_TABLE_ID = "01ARZ3NDEKTSV4RRFFQ69G5OTT";
  private static final String COLUMN_ID_1 = "01ARZ3NDEKTSV4RRFFQ69G5CL1";
  private static final String COLUMN_ID_2 = "01ARZ3NDEKTSV4RRFFQ69G5CL2";
  private static final String COLUMN_ID_3 = "01ARZ3NDEKTSV4RRFFQ69G5CL3";

  @Autowired
  ColumnPersistenceAdapter sut;

  @Autowired
  ColumnRepository columnRepository;

  @BeforeEach
  void setUp() {
    columnRepository.deleteAll().block();
  }

  @Nested
  @DisplayName("createColumn 메서드는")
  class CreateColumn {

    @Test
    @DisplayName("컬럼을 저장하고 반환한다")
    void savesAndReturnsColumn() {
      var column = ColumnFixture.defaultColumn();

      StepVerifier.create(sut.createColumn(column))
          .assertNext(saved -> {
            assertThat(saved.id()).isEqualTo(column.id());
            assertThat(saved.tableId()).isEqualTo(column.tableId());
            assertThat(saved.name()).isEqualTo(column.name());
            assertThat(saved.dataType()).isEqualTo(column.dataType());
            assertThat(saved.lengthScale().length()).isEqualTo(ColumnFixture.DEFAULT_LENGTH);
            assertThat(saved.seqNo()).isEqualTo(column.seqNo());
            assertThat(saved.autoIncrement()).isEqualTo(column.autoIncrement());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("DECIMAL 컬럼을 저장한다")
    void savesDecimalColumn() {
      var column = ColumnFixture.decimalColumn();

      StepVerifier.create(sut.createColumn(column))
          .assertNext(saved -> {
            assertThat(saved.dataType()).isEqualTo("DECIMAL");
            assertThat(saved.lengthScale().precision()).isEqualTo(10);
            assertThat(saved.lengthScale().scale()).isEqualTo(2);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("INT 컬럼을 저장한다")
    void savesIntColumn() {
      var column = ColumnFixture.intColumn();

      StepVerifier.create(sut.createColumn(column))
          .assertNext(saved -> {
            assertThat(saved.dataType()).isEqualTo("INT");
            assertThat(saved.lengthScale()).isNull();
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findColumnById 메서드는")
  class FindColumnById {

    @Test
    @DisplayName("존재하는 컬럼을 반환한다")
    void returnsExistingColumn() {
      var column = ColumnFixture.defaultColumn();
      sut.createColumn(column).block();

      StepVerifier.create(sut.findColumnById(column.id()))
          .assertNext(found -> {
            assertThat(found.id()).isEqualTo(column.id());
            assertThat(found.name()).isEqualTo(column.name());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 empty를 반환한다")
    void returnsEmptyWhenNotExists() {
      StepVerifier.create(sut.findColumnById("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findColumnsByTableId 메서드는")
  class FindColumnsByTableId {

    @Test
    @DisplayName("테이블의 컬럼들을 seqNo 순서로 반환한다")
    void returnsColumnsBySeqNoOrder() {
      var column1 = new Column(
          COLUMN_ID_1,
          ColumnFixture.DEFAULT_TABLE_ID,
          "column_a",
          "VARCHAR",
          new ColumnLengthScale(255, null, null),
          2,
          false,
          null,
          null,
          null);
      var column2 = new Column(
          COLUMN_ID_2,
          ColumnFixture.DEFAULT_TABLE_ID,
          "column_b",
          "INT",
          null,
          0,
          false,
          null,
          null,
          null);
      var column3 = new Column(
          COLUMN_ID_3,
          ColumnFixture.DEFAULT_TABLE_ID,
          "column_c",
          "TEXT",
          null,
          1,
          false,
          null,
          null,
          null);

      sut.createColumn(column1).block();
      sut.createColumn(column2).block();
      sut.createColumn(column3).block();

      StepVerifier.create(sut.findColumnsByTableId(ColumnFixture.DEFAULT_TABLE_ID))
          .assertNext(columns -> {
            assertThat(columns).hasSize(3);
            assertThat(columns.get(0).seqNo()).isEqualTo(0);
            assertThat(columns.get(1).seqNo()).isEqualTo(1);
            assertThat(columns.get(2).seqNo()).isEqualTo(2);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 테이블의 컬럼은 반환하지 않는다")
    void returnsOnlyColumnsOfSpecifiedTable() {
      var column1 = ColumnFixture.defaultColumn();
      var column2 = new Column(
          OTHER_COLUMN_ID,
          OTHER_TABLE_ID,
          "other_column",
          "INT",
          null,
          0,
          false,
          null,
          null,
          null);

      sut.createColumn(column1).block();
      sut.createColumn(column2).block();

      StepVerifier.create(sut.findColumnsByTableId(ColumnFixture.DEFAULT_TABLE_ID))
          .assertNext(columns -> {
            assertThat(columns).hasSize(1);
            assertThat(columns.get(0).id()).isEqualTo(column1.id());
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeColumnName 메서드는")
  class ChangeColumnName {

    @Test
    @DisplayName("컬럼 이름을 변경한다")
    void changesColumnName() {
      var column = ColumnFixture.defaultColumn();
      sut.createColumn(column).block();
      var newName = "new_column_name";

      StepVerifier.create(sut.changeColumnName(column.id(), newName))
          .verifyComplete();

      StepVerifier.create(sut.findColumnById(column.id()))
          .assertNext(found -> assertThat(found.name()).isEqualTo(newName))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 컬럼이면 예외가 발생한다")
    void throwsWhenColumnNotExists() {
      StepVerifier.create(sut.changeColumnName("non-existent-id", "new_name"))
          .expectErrorMatches(DomainException.hasErrorCode(ColumnErrorCode.NOT_FOUND))
          .verify();
    }

  }

  @Nested
  @DisplayName("changeColumnType 메서드는")
  class ChangeColumnType {

    @Test
    @DisplayName("컬럼 타입을 변경한다")
    void changesColumnType() {
      var column = ColumnFixture.intColumn();
      sut.createColumn(column).block();
      var newType = "BIGINT";

      StepVerifier.create(sut.changeColumnType(column.id(), newType, null))
          .verifyComplete();

      StepVerifier.create(sut.findColumnById(column.id()))
          .assertNext(found -> {
            assertThat(found.dataType()).isEqualTo(newType);
            assertThat(found.lengthScale()).isNull();
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("타입을 변경하면 dataType이 업데이트된다")
    void changesColumnDataType() {
      var column = ColumnFixture.defaultColumn();
      sut.createColumn(column).block();
      var newType = "TEXT";

      StepVerifier.create(sut.changeColumnType(column.id(), newType, null))
          .verifyComplete();

      StepVerifier.create(sut.findColumnById(column.id()))
          .assertNext(found -> assertThat(found.dataType()).isEqualTo(newType))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeColumnMeta 메서드는")
  class ChangeColumnMeta {

    @Test
    @DisplayName("autoIncrement만 변경한다")
    void changesOnlyAutoIncrement() {
      var column = ColumnFixture.intColumn();
      sut.createColumn(column).block();

      StepVerifier.create(sut.changeColumnMeta(column.id(), true, null, null, null))
          .verifyComplete();

      StepVerifier.create(sut.findColumnById(column.id()))
          .assertNext(found -> {
            assertThat(found.autoIncrement()).isTrue();
            assertThat(found.charset()).isNull();
            assertThat(found.collation()).isNull();
            assertThat(found.comment()).isNull();
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("charset/collation을 변경한다")
    void changesCharsetAndCollation() {
      var column = ColumnFixture.defaultColumn();
      sut.createColumn(column).block();

      StepVerifier.create(sut.changeColumnMeta(column.id(), null, "utf8mb4", "utf8mb4_general_ci", null))
          .verifyComplete();

      StepVerifier.create(sut.findColumnById(column.id()))
          .assertNext(found -> {
            assertThat(found.charset()).isEqualTo("utf8mb4");
            assertThat(found.collation()).isEqualTo("utf8mb4_general_ci");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("comment를 변경한다")
    void changesComment() {
      var column = ColumnFixture.defaultColumn();
      sut.createColumn(column).block();
      var comment = "Test comment";

      StepVerifier.create(sut.changeColumnMeta(column.id(), null, null, null, comment))
          .verifyComplete();

      StepVerifier.create(sut.findColumnById(column.id()))
          .assertNext(found -> assertThat(found.comment()).isEqualTo(comment))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeColumnPositions 메서드는")
  class ChangeColumnPosition {

    @Test
    @DisplayName("테이블의 컬럼 위치를 일괄 변경한다")
    void changesColumnPositions() {
      var column1 = new Column(
          COLUMN_ID_1,
          ColumnFixture.DEFAULT_TABLE_ID,
          "column_a",
          "VARCHAR",
          new ColumnLengthScale(255, null, null),
          0,
          false,
          null,
          null,
          null);
      var column2 = new Column(
          COLUMN_ID_2,
          ColumnFixture.DEFAULT_TABLE_ID,
          "column_b",
          "VARCHAR",
          new ColumnLengthScale(255, null, null),
          1,
          false,
          null,
          null,
          null);
      var column3 = new Column(
          COLUMN_ID_3,
          ColumnFixture.DEFAULT_TABLE_ID,
          "column_c",
          "VARCHAR",
          new ColumnLengthScale(255, null, null),
          2,
          false,
          null,
          null,
          null);
      sut.createColumn(column1).block();
      sut.createColumn(column2).block();
      sut.createColumn(column3).block();

      var reordered = List.of(
          new Column(
              column3.id(),
              column3.tableId(),
              column3.name(),
              column3.dataType(),
              column3.lengthScale(),
              0,
              column3.autoIncrement(),
              column3.charset(),
              column3.collation(),
              column3.comment()),
          new Column(
              column1.id(),
              column1.tableId(),
              column1.name(),
              column1.dataType(),
              column1.lengthScale(),
              1,
              column1.autoIncrement(),
              column1.charset(),
              column1.collation(),
              column1.comment()),
          new Column(
              column2.id(),
              column2.tableId(),
              column2.name(),
              column2.dataType(),
              column2.lengthScale(),
              2,
              column2.autoIncrement(),
              column2.charset(),
              column2.collation(),
              column2.comment()));

      StepVerifier.create(sut.changeColumnPositions(ColumnFixture.DEFAULT_TABLE_ID, reordered))
          .verifyComplete();

      StepVerifier.create(sut.findColumnsByTableId(ColumnFixture.DEFAULT_TABLE_ID))
          .assertNext(columns -> {
            assertThat(columns).hasSize(3);
            assertThat(columns.get(0).id()).isEqualTo(COLUMN_ID_3);
            assertThat(columns.get(0).seqNo()).isEqualTo(0);
            assertThat(columns.get(1).id()).isEqualTo(COLUMN_ID_1);
            assertThat(columns.get(1).seqNo()).isEqualTo(1);
            assertThat(columns.get(2).id()).isEqualTo(COLUMN_ID_2);
            assertThat(columns.get(2).seqNo()).isEqualTo(2);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 테이블 컬럼에는 영향을 주지 않는다")
    void doesNotAffectOtherTable() {
      var targetColumn = ColumnFixture.defaultColumn();
      var otherColumn = new Column(
          OTHER_COLUMN_ID,
          OTHER_TABLE_ID,
          "other_column",
          "INT",
          null,
          0,
          false,
          null,
          null,
          null);
      sut.createColumn(targetColumn).block();
      sut.createColumn(otherColumn).block();

      var reordered = List.of(
          new Column(
              targetColumn.id(),
              targetColumn.tableId(),
              targetColumn.name(),
              targetColumn.dataType(),
              targetColumn.lengthScale(),
              0,
              targetColumn.autoIncrement(),
              targetColumn.charset(),
              targetColumn.collation(),
              targetColumn.comment()));

      StepVerifier.create(sut.changeColumnPositions(targetColumn.tableId(), reordered))
          .verifyComplete();

      StepVerifier.create(sut.findColumnById(OTHER_COLUMN_ID))
          .assertNext(found -> {
            assertThat(found.tableId()).isEqualTo(OTHER_TABLE_ID);
            assertThat(found.seqNo()).isEqualTo(0);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("빈 목록이면 아무 작업도 하지 않는다")
    void returnsWhenColumnsEmpty() {
      StepVerifier.create(sut.changeColumnPositions(ColumnFixture.DEFAULT_TABLE_ID, List.of()))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("deleteColumn 메서드는")
  class DeleteColumn {

    @Test
    @DisplayName("컬럼을 삭제한다")
    void deletesColumn() {
      var column = ColumnFixture.defaultColumn();
      sut.createColumn(column).block();

      StepVerifier.create(sut.deleteColumn(column.id()))
          .verifyComplete();

      StepVerifier.create(sut.findColumnById(column.id()))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("deleteColumnsByTableId 메서드는")
  class DeleteColumnsByTableId {

    @Test
    @DisplayName("테이블의 모든 컬럼을 삭제한다")
    void deletesAllColumnsOfTable() {
      var column1 = ColumnFixture.columnWithId(COLUMN_ID_1);
      var column2 = ColumnFixture.columnWithId(COLUMN_ID_2);
      sut.createColumn(column1).block();
      sut.createColumn(column2).block();

      StepVerifier.create(sut.deleteColumnsByTableId(ColumnFixture.DEFAULT_TABLE_ID))
          .verifyComplete();

      StepVerifier.create(sut.findColumnsByTableId(ColumnFixture.DEFAULT_TABLE_ID))
          .assertNext(columns -> assertThat(columns).isEmpty())
          .verifyComplete();
    }

    @Test
    @DisplayName("다른 테이블의 컬럼은 삭제하지 않는다")
    void doesNotDeleteColumnsOfOtherTables() {
      var column1 = ColumnFixture.defaultColumn();
      var column2 = new Column(
          OTHER_COLUMN_ID,
          OTHER_TABLE_ID,
          "other_column",
          "INT",
          null,
          0,
          false,
          null,
          null,
          null);
      sut.createColumn(column1).block();
      sut.createColumn(column2).block();

      StepVerifier.create(sut.deleteColumnsByTableId(ColumnFixture.DEFAULT_TABLE_ID))
          .verifyComplete();

      StepVerifier.create(sut.findColumnById(column2.id()))
          .assertNext(found -> assertThat(found.id()).isEqualTo(OTHER_COLUMN_ID))
          .verifyComplete();
    }

  }

}
