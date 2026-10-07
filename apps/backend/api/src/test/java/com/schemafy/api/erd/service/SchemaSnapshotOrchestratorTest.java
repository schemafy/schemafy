package com.schemafy.api.erd.service;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemafy.api.erd.controller.dto.response.TableResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.erd.schema.domain.Schema;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchemaSnapshotOrchestrator")
class SchemaSnapshotOrchestratorTest {

  @Mock
  SchemaSnapshotReader schemaSnapshotReader;

  SchemaSnapshotOrchestrator sut;

  @BeforeEach
  void setUp() {
    sut = new SchemaSnapshotOrchestrator(schemaSnapshotReader);
  }

  @Test
  @DisplayName("공통 read 결과를 schema state로 변환하고 schema에 revision을 넣지 않는다")
  void convertsReadResultToSchemaState() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "main_schema",
        "utf8mb4", "utf8mb4_general_ci");
    TableSnapshotResponse snapshot = tableSnapshot("table-1", schemaId,
        "users");

    given(schemaSnapshotReader.readSchemaSnapshot(schemaId))
        .willReturn(Mono.just(new SchemaSnapshotReadResult(schema, 42L,
            Map.of("table-1", snapshot))));

    StepVerifier.create(sut.getSchemaState(schemaId))
        .assertNext(result -> {
          assertThat(result.schema().id()).isEqualTo(schemaId);
          assertThat(result.schema().projectId()).isEqualTo("project-1");
          assertThat(result.schema().name()).isEqualTo("main_schema");
          assertThat(result.schema().charset()).isEqualTo("utf8mb4");
          assertThat(result.schema().collation())
              .isEqualTo("utf8mb4_general_ci");
          assertThat(result.schema().currentRevision()).isNull();
          assertThat(result.revision()).isEqualTo(42L);
          assertThat(result.snapshots()).containsEntry("table-1", snapshot);
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("공통 read 결과를 snapshots 응답으로 변환한다")
  void convertsReadResultToSnapshotsResponse() {
    String schemaId = "schema-1";
    Schema schema = new Schema(schemaId, "project-1", "empty_schema",
        "utf8mb4", "utf8mb4_general_ci");

    given(schemaSnapshotReader.readSchemaSnapshot(schemaId))
        .willReturn(Mono.just(
            new SchemaSnapshotReadResult(schema, 7L, Map.of())));

    StepVerifier.create(sut.getSchemaSnapshots(schemaId))
        .assertNext(result -> {
          assertThat(result.currentRevision()).isEqualTo(7L);
          assertThat(result.snapshots()).isEmpty();
        })
        .verifyComplete();
  }

  @Test
  @DisplayName("공통 read 실패는 그대로 전파한다")
  void propagatesReadFailure() {
    String schemaId = "schema-1";

    given(schemaSnapshotReader.readSchemaSnapshot(schemaId))
        .willReturn(Mono.error(new IllegalStateException("snapshot failed")));

    StepVerifier.create(sut.getSchemaSnapshots(schemaId))
        .expectErrorMessage("snapshot failed")
        .verify();
  }

  private static TableSnapshotResponse tableSnapshot(String tableId,
      String schemaId, String name) {
    return new TableSnapshotResponse(
        new TableResponse(tableId, schemaId, name, "utf8mb4",
            "utf8mb4_general_ci", null),
        List.of(),
        List.of(),
        List.of(),
        List.of());
  }

}
