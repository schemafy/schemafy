package com.schemafy.domain.erd.table.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.domain.config.R2dbcTestConfiguration;
import com.schemafy.domain.erd.table.domain.exception.TableNotExistException;
import com.schemafy.domain.erd.table.fixture.TableFixture;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({ TablePersistenceAdapter.class, TableMapper.class, R2dbcTestConfiguration.class })
@DisplayName("TablePersistenceAdapter")
class TablePersistenceAdapterTest {

  @Autowired
  TablePersistenceAdapter sut;

  @Autowired
  TableRepository tableRepository;

  @BeforeEach
  void setUp() {
    tableRepository.deleteAll().block();
  }

  @Nested
  @DisplayName("createTable 메서드는")
  class CreateTable {

    @Test
    @DisplayName("테이블을 저장하고 반환한다")
    void savesAndReturnsTable() {
      var table = TableFixture.defaultTable();

      StepVerifier.create(sut.createTable(table))
          .assertNext(saved -> {
            assertThat(saved.id()).isEqualTo(table.id());
            assertThat(saved.schemaId()).isEqualTo(table.schemaId());
            assertThat(saved.name()).isEqualTo(table.name());
            assertThat(saved.charset()).isEqualTo(table.charset());
            assertThat(saved.collation()).isEqualTo(table.collation());
          })
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findTableById 메서드는")
  class FindTableById {

    @Test
    @DisplayName("존재하는 테이블을 반환한다")
    void returnsExistingTable() {
      var table = TableFixture.defaultTable();
      sut.createTable(table).block();

      StepVerifier.create(sut.findTableById(table.id()))
          .assertNext(found -> {
            assertThat(found.id()).isEqualTo(table.id());
            assertThat(found.name()).isEqualTo(table.name());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 empty를 반환한다")
    void returnsEmptyWhenNotExists() {
      StepVerifier.create(sut.findTableById("non-existent-id"))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findTablesBySchemaId 메서드는")
  class FindTablesBySchemaId {

    @Test
    @DisplayName("스키마의 테이블 목록을 반환한다")
    void returnsTablesForSchema() {
      var table1 = TableFixture.tableWithIdAndName("01ARZ3NDEKTSV4RRFFQ69G5TA1", "table_1");
      var table2 = TableFixture.tableWithIdAndName("01ARZ3NDEKTSV4RRFFQ69G5TA2", "table_2");
      sut.createTable(table1).block();
      sut.createTable(table2).block();

      StepVerifier.create(sut.findTablesBySchemaId(TableFixture.DEFAULT_SCHEMA_ID).collectList())
          .assertNext(tables -> assertThat(tables).hasSize(2))
          .verifyComplete();
    }

    @Test
    @DisplayName("테이블이 없으면 빈 Flux를 반환한다")
    void returnsEmptyFluxWhenNoTables() {
      StepVerifier.create(sut.findTablesBySchemaId("non-existent-schema-id").collectList())
          .assertNext(tables -> assertThat(tables).isEmpty())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("existsBySchemaIdAndName 메서드는")
  class ExistsBySchemaIdAndName {

    @Test
    @DisplayName("존재하면 true를 반환한다")
    void returnsTrueWhenExists() {
      var table = TableFixture.defaultTable();
      sut.createTable(table).block();

      StepVerifier.create(sut.existsBySchemaIdAndName(table.schemaId(), table.name()))
          .assertNext(exists -> assertThat(exists).isTrue())
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 false를 반환한다")
    void returnsFalseWhenNotExists() {
      StepVerifier.create(sut.existsBySchemaIdAndName(
          "non-existent-schema", "non-existent-name"))
          .assertNext(exists -> assertThat(exists).isFalse())
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("changeTableName 메서드는")
  class ChangeTableName {

    @Test
    @DisplayName("테이블 이름을 변경한다")
    void changesTableName() {
      var table = TableFixture.defaultTable();
      sut.createTable(table).block();
      var newName = "updated_table_name";

      StepVerifier.create(sut.changeTableName(table.id(), newName))
          .verifyComplete();

      StepVerifier.create(sut.findTableById(table.id()))
          .assertNext(found -> assertThat(found.name()).isEqualTo(newName))
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 테이블이면 예외를 발생시킨다")
    void throwsWhenTableNotExists() {
      StepVerifier.create(sut.changeTableName("non-existent-id", "new_name"))
          .expectError(TableNotExistException.class)
          .verify();
    }

  }

  @Nested
  @DisplayName("changeTableMeta 메서드는")
  class ChangeTableMeta {

    @Test
    @DisplayName("charset과 collation을 변경한다")
    void changesCharsetAndCollation() {
      var table = TableFixture.defaultTable();
      sut.createTable(table).block();
      var newCharset = "utf8";
      var newCollation = "utf8_general_ci";

      StepVerifier.create(sut.changeTableMeta(table.id(), newCharset, newCollation))
          .verifyComplete();

      StepVerifier.create(sut.findTableById(table.id()))
          .assertNext(found -> {
            assertThat(found.charset()).isEqualTo(newCharset);
            assertThat(found.collation()).isEqualTo(newCollation);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("charset만 변경하면 collation은 유지된다")
    void changesOnlyCharset() {
      var table = TableFixture.defaultTable();
      sut.createTable(table).block();
      var newCharset = "utf8";

      StepVerifier.create(sut.changeTableMeta(table.id(), newCharset, null))
          .verifyComplete();

      StepVerifier.create(sut.findTableById(table.id()))
          .assertNext(found -> {
            assertThat(found.charset()).isEqualTo(newCharset);
            assertThat(found.collation()).isEqualTo(table.collation());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("collation만 변경하면 charset은 유지된다")
    void changesOnlyCollation() {
      var table = TableFixture.defaultTable();
      sut.createTable(table).block();
      var newCollation = "utf8_general_ci";

      StepVerifier.create(sut.changeTableMeta(table.id(), null, newCollation))
          .verifyComplete();

      StepVerifier.create(sut.findTableById(table.id()))
          .assertNext(found -> {
            assertThat(found.charset()).isEqualTo(table.charset());
            assertThat(found.collation()).isEqualTo(newCollation);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("둘 다 null이면 기존 값이 유지된다")
    void keepsExistingValuesWhenBothNull() {
      var table = TableFixture.defaultTable();
      sut.createTable(table).block();

      StepVerifier.create(sut.changeTableMeta(table.id(), null, null))
          .verifyComplete();

      StepVerifier.create(sut.findTableById(table.id()))
          .assertNext(found -> {
            assertThat(found.charset()).isEqualTo(table.charset());
            assertThat(found.collation()).isEqualTo(table.collation());
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 테이블이면 예외를 발생시킨다")
    void throwsWhenTableNotExists() {
      StepVerifier.create(sut.changeTableMeta("non-existent-id", "utf8", "utf8_general_ci"))
          .expectError(TableNotExistException.class)
          .verify();
    }

  }

  @Nested
  @DisplayName("changeTableExtra 메서드는")
  class ChangeTableExtra {

    @Test
    @DisplayName("extra 필드를 변경한다")
    void changesExtra() {
      var table = TableFixture.defaultTable();
      sut.createTable(table).block();
      var newExtra = "{\"ui\": {\"x\": 100, \"y\": 200}}";

      StepVerifier.create(sut.changeTableExtra(table.id(), newExtra))
          .verifyComplete();

      StepVerifier.create(tableRepository.findById(table.id()))
          .assertNext(found -> {
            assertThat(found.getExtra()).isNotNull();
            assertThat(found.getExtra()).contains("ui");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 테이블이면 예외를 발생시킨다")
    void throwsWhenTableNotExists() {
      StepVerifier.create(sut.changeTableExtra("non-existent-id", "{\"ui\": {}}"))
          .expectError(TableNotExistException.class)
          .verify();
    }

  }

  @Nested
  @DisplayName("deleteTable 메서드는")
  class DeleteTable {

    @Test
    @DisplayName("테이블을 삭제한다")
    void deletesTable() {
      var table = TableFixture.defaultTable();
      sut.createTable(table).block();

      StepVerifier.create(sut.deleteTable(table.id()))
          .verifyComplete();

      StepVerifier.create(sut.findTableById(table.id()))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("cascadeDeleteBySchemaId 메서드는")
  class CascadeDeleteBySchemaId {

    @Test
    @DisplayName("스키마의 모든 테이블을 삭제한다")
    void deletesAllTablesForSchema() {
      var table1 = TableFixture.tableWithIdAndName("01ARZ3NDEKTSV4RRFFQ69G5TA1", "table_1");
      var table2 = TableFixture.tableWithIdAndName("01ARZ3NDEKTSV4RRFFQ69G5TA2", "table_2");
      sut.createTable(table1).block();
      sut.createTable(table2).block();

      StepVerifier.create(sut.cascadeDeleteBySchemaId(TableFixture.DEFAULT_SCHEMA_ID))
          .verifyComplete();

      StepVerifier.create(sut.findTablesBySchemaId(TableFixture.DEFAULT_SCHEMA_ID).collectList())
          .assertNext(tables -> assertThat(tables).isEmpty())
          .verifyComplete();
    }

  }

}
