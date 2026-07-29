package com.schemafy.core.erd.vendor.adapter.out.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.core.DatabaseClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.schemafy.core.config.R2dbcTestConfiguration;
import com.schemafy.core.erd.index.domain.type.IndexType;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import({ DbVendorPersistenceAdapter.class, DbVendorMapper.class, R2dbcTestConfiguration.class })
@DisplayName("DbVendorPersistenceAdapter")
class DbVendorPersistenceAdapterTest {

  private static final Integer DB_VENDOR_ID = 1;
  private static final Integer DB_VENDOR_84_ID = 2;
  private static final Integer DELETED_DB_VENDOR_ID = 3;

  @Autowired
  DbVendorPersistenceAdapter sut;

  @Autowired
  DatabaseClient databaseClient;

  @BeforeEach
  void setUp() {
    cleanUpTestVendors();
  }

  @AfterEach
  void tearDown() {
    cleanUpTestVendors();
  }

  private void cleanUpTestVendors() {
    databaseClient.sql("DELETE FROM projects")
        .fetch()
        .rowsUpdated()
        .then(databaseClient.sql("DELETE FROM db_vendors WHERE id <> :id")
            .bind("id", DB_VENDOR_ID)
            .fetch()
            .rowsUpdated())
        .block();
  }

  @Nested
  @DisplayName("findAllSummaries 메서드는")
  class FindAllSummaries {

    @Test
    @DisplayName("시드 데이터의 벤더 요약을 반환한다")
    void returnsSeedVendorSummaries() {
      StepVerifier.create(sut.findAllSummaries())
          .assertNext(summary -> {
            assertThat(summary.id()).isEqualTo(DB_VENDOR_ID);
            assertThat(summary.displayName()).isEqualTo("MySQL 8.0");
            assertThat(summary.name()).isEqualTo("mysql");
            assertThat(summary.version()).isEqualTo("8.0");
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("삭제된 벤더를 제외하고 안정된 순서로 반환한다")
    void returnsActiveVendorsInStableOrder() {
      insertVendor(DB_VENDOR_84_ID, "MySQL 8.4", "mysql", "8.4").block();
      insertVendor(DELETED_DB_VENDOR_ID, "MySQL 5.7", "mysql", "5.7").block();
      softDeleteVendor(DELETED_DB_VENDOR_ID).block();

      StepVerifier.create(sut.findAllSummaries().map(summary -> summary.id()).collectList())
          .assertNext(ids -> assertThat(ids).containsExactly(DB_VENDOR_ID, DB_VENDOR_84_ID))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("findActiveById 메서드는")
  class FindActiveById {

    @Test
    @DisplayName("존재하는 벤더를 반환한다")
    void returnsExistingVendor() {
      StepVerifier.create(sut.findActiveById(DB_VENDOR_ID))
          .assertNext(vendor -> {
            assertThat(vendor.id()).isEqualTo(DB_VENDOR_ID);
            assertThat(vendor.displayName()).isEqualTo("MySQL 8.0");
            assertThat(vendor.name()).isEqualTo("mysql");
            assertThat(vendor.version()).isEqualTo("8.0");
            assertThat(vendor.datatypeMappings()).contains("schemaVersion");
            assertThat(vendor.datatypeMappings()).contains("TINYINT");
            assertThat(vendor.capabilities().schemaVersion()).isEqualTo(1);
            assertThat(vendor.capabilities().indexes().supportedTypes())
                .containsExactlyInAnyOrder(
                    IndexType.BTREE,
                    IndexType.FULLTEXT,
                    IndexType.SPATIAL);
            assertThat(vendor.capabilities().indexes().sortDirectionTypes())
                .containsExactly(IndexType.BTREE);
          })
          .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않으면 empty를 반환한다")
    void returnsEmptyWhenNotExists() {
      StepVerifier.create(sut.findActiveById(999))
          .verifyComplete();
    }

    @Test
    @DisplayName("삭제된 벤더이면 empty를 반환한다")
    void returnsEmptyWhenDeleted() {
      insertVendor(DELETED_DB_VENDOR_ID, "MySQL 5.7", "mysql", "5.7").block();
      softDeleteVendor(DELETED_DB_VENDOR_ID).block();

      StepVerifier.create(sut.findActiveById(DELETED_DB_VENDOR_ID))
          .verifyComplete();
    }

  }

  @Nested
  @DisplayName("벤더 식별 제약은")
  class VendorIdentityConstraint {

    @Test
    @DisplayName("ID를 생략하면 숫자 ID를 자동 생성한다")
    void generatesNumericIdWhenIdIsOmitted() {
      StepVerifier.create(insertVendorWithoutId("MySQL 9.0", "mysql", "9.0"))
          .assertNext(id -> assertThat(id)
              .isPositive()
              .isNotEqualTo(DB_VENDOR_ID))
          .verifyComplete();
    }

    @Test
    @DisplayName("같은 name의 다른 version을 허용한다")
    void allowsSameNameWithDifferentVersion() {
      StepVerifier.create(insertVendor(
          DB_VENDOR_84_ID, "MySQL 8.4", "mysql", "8.4"))
          .expectNext(1L)
          .verifyComplete();
    }

    @Test
    @DisplayName("같은 name과 version 조합을 거부한다")
    void rejectsDuplicateNameAndVersion() {
      StepVerifier.create(insertVendor(
          4, "Another Label", "mysql", "8.0"))
          .expectError(DataIntegrityViolationException.class)
          .verify();
    }

  }

  @Nested
  @DisplayName("프로젝트 벤더 제약은")
  class ProjectVendorConstraint {

    @Test
    @DisplayName("db_vendor_id가 없는 프로젝트 저장을 거부한다")
    void rejectsProjectWithoutDbVendorId() {
      StepVerifier.create(databaseClient.sql("""
          INSERT INTO projects (id, workspace_id, name)
          VALUES ('project-without-vendor', 'workspace-id', 'Project')
          """)
          .fetch()
          .rowsUpdated())
          .expectError(DataIntegrityViolationException.class)
          .verify();
    }

    @Test
    @DisplayName("존재하지 않는 db_vendor_id의 프로젝트 저장을 거부한다")
    void rejectsProjectWithMissingDbVendorId() {
      StepVerifier.create(databaseClient.sql("""
          INSERT INTO projects (id, workspace_id, db_vendor_id, name)
          VALUES ('project-missing-vendor', 'workspace-id', 999, 'Project')
          """)
          .fetch()
          .rowsUpdated())
          .expectError(DataIntegrityViolationException.class)
          .verify();
    }

  }

  private Mono<Long> insertVendor(
      Integer id,
      String displayName,
      String name,
      String version) {
    return databaseClient.sql(
        """
            INSERT INTO db_vendors (
              id, display_name, name, version, datatype_mappings, capabilities
            ) VALUES (
              :id, :displayName, :name, :version, '{}',
              '{"schemaVersion":1,"indexes":{"supportedTypes":["BTREE","FULLTEXT","SPATIAL"],"sortDirectionTypes":["BTREE"]}}'
            )
            """)
        .bind("id", id)
        .bind("displayName", displayName)
        .bind("name", name)
        .bind("version", version)
        .fetch()
        .rowsUpdated();
  }

  private Mono<Integer> insertVendorWithoutId(
      String displayName,
      String name,
      String version) {
    return databaseClient.sql(
        """
            INSERT INTO db_vendors (
              display_name, name, version, datatype_mappings, capabilities
            ) VALUES (
              :displayName, :name, :version, '{}',
              '{"schemaVersion":1,"indexes":{"supportedTypes":["BTREE","FULLTEXT","SPATIAL"],"sortDirectionTypes":["BTREE"]}}'
            )
            """)
        .bind("displayName", displayName)
        .bind("name", name)
        .bind("version", version)
        .fetch()
        .rowsUpdated()
        .then(databaseClient.sql("SELECT id FROM db_vendors WHERE name = :name AND version = :version")
            .bind("name", name)
            .bind("version", version)
            .map((row, metadata) -> row.get("id", Integer.class))
            .one());
  }

  private Mono<Long> softDeleteVendor(Integer id) {
    return databaseClient.sql("UPDATE db_vendors SET deleted_at = CURRENT_TIMESTAMP WHERE id = :id")
        .bind("id", id)
        .fetch()
        .rowsUpdated();
  }

}
