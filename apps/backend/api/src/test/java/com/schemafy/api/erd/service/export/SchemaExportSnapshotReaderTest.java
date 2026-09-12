package com.schemafy.api.erd.service.export;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.erd.controller.dto.response.ColumnResponse;
import com.schemafy.api.erd.controller.dto.response.TableResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.api.erd.fixture.DbVendorApiFixture;
import com.schemafy.api.erd.service.SchemaSnapshotReadResult;
import com.schemafy.api.erd.service.SchemaSnapshotReader;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;
import com.schemafy.core.erd.index.domain.type.IndexType;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorUseCase;
import com.schemafy.core.erd.vendor.domain.DbVendor;
import com.schemafy.core.erd.vendor.domain.IdentifierCapabilities;
import com.schemafy.core.erd.vendor.domain.VendorCapabilities;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaExportSnapshotReader")
class SchemaExportSnapshotReaderTest {

  @Mock
  SchemaSnapshotReader schemaSnapshotReader;

  @Mock
  GetProjectDbVendorUseCase getProjectDbVendorUseCase;

  SchemaExportSnapshotReader sut;

  @BeforeEach
  void setUp() {
    sut = new SchemaExportSnapshotReader(
        schemaSnapshotReader,
        getProjectDbVendorUseCase,
        new SchemaExportSnapshotMapper());
  }

  @Test
  @DisplayName("공통 read 결과에 vendor capability를 결합해 export snapshot으로 변환한다")
  void readsSchemaExportSnapshot() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "main_schema",
        "utf8mb4", "utf8mb4_general_ci");
    TableSnapshotResponse tableSnapshot = new TableSnapshotResponse(
        new TableResponse("table-1", schemaId, "users", "utf8mb4",
            "utf8mb4_general_ci", null),
        List.of(new ColumnResponse(
            "column-1", "table-1", "id", "BIGINT", null, 0, true,
            null, null, null)),
        List.of(),
        List.of(),
        List.of());

    given(schemaSnapshotReader.readSchemaSnapshot(schemaId))
        .willReturn(Mono.just(new SchemaSnapshotReadResult(schema, 42L,
            Map.of("table-1", tableSnapshot))));
    given(getProjectDbVendorUseCase.getProjectDbVendor(
        new GetProjectDbVendorQuery(schema.projectId())))
        .willReturn(Mono.just(sourceDbVendor()));

    StepVerifier.create(sut.readSchemaExportSnapshot(schemaId))
        .assertNext(result -> {
          assertThat(result.currentRevision()).isEqualTo(42L);
          assertThat(result.snapshot().schema().id()).isEqualTo(schemaId);
          assertThat(result.snapshot().schema().dbVendorName())
              .isEqualTo("mysql");
          assertThat(result.datatypePolicy())
              .isEqualTo(DbVendorApiFixture.mysqlDatatypePolicy());
          assertThat(result.indexCapabilities())
              .isEqualTo(mysqlIndexCapabilities());
          assertThat(result.identifierCapabilities())
              .isEqualTo(IdentifierCapabilities.codePoints(64));
          assertThat(result.snapshot().tables()).hasSize(1);
          assertThat(result.snapshot().tables().getFirst().columns())
              .hasSize(1);
        })
        .verifyComplete();

    then(getProjectDbVendorUseCase).should()
        .getProjectDbVendor(new GetProjectDbVendorQuery(schema.projectId()));
  }

  @Test
  @DisplayName("테이블이 없으면 빈 export snapshot을 반환한다")
  void readsEmptySchemaExportSnapshot() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "empty_schema",
        "utf8mb4", "utf8mb4_general_ci");

    given(schemaSnapshotReader.readSchemaSnapshot(schemaId))
        .willReturn(Mono.just(
            new SchemaSnapshotReadResult(schema, 7L, Map.of())));
    given(getProjectDbVendorUseCase.getProjectDbVendor(
        new GetProjectDbVendorQuery(schema.projectId())))
        .willReturn(Mono.just(sourceDbVendor()));

    StepVerifier.create(sut.readSchemaExportSnapshot(schemaId))
        .assertNext(result -> {
          assertThat(result.currentRevision()).isEqualTo(7L);
          assertThat(result.datatypePolicy())
              .isEqualTo(DbVendorApiFixture.mysqlDatatypePolicy());
          assertThat(result.snapshot().tables()).isEmpty();
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("공통 read 실패를 전파하고 vendor는 조회하지 않는다")
  void propagatesReadFailureWithoutVendorLookup() {
    String schemaId = "schema-1";
    RuntimeException failure = new RuntimeException("snapshot failure");

    given(schemaSnapshotReader.readSchemaSnapshot(schemaId))
        .willReturn(Mono.error(failure));

    StepVerifier.create(sut.readSchemaExportSnapshot(schemaId))
        .expectErrorMatches(error -> error == failure)
        .verify();

    then(getProjectDbVendorUseCase).shouldHaveNoInteractions();
  }

  private static DbVendor sourceDbVendor() {
    return new DbVendor(
        1,
        "MySQL 8.0",
        "mysql",
        "8.0",
        DbVendorApiFixture.mysqlDatatypePolicy(),
        new VendorCapabilities(
            2,
            mysqlIndexCapabilities(),
            IdentifierCapabilities.codePoints(64)));
  }

  private static IndexCapabilities mysqlIndexCapabilities() {
    return new IndexCapabilities(
        Set.of(IndexType.BTREE, IndexType.FULLTEXT, IndexType.SPATIAL),
        Set.of(IndexType.BTREE));
  }

}
