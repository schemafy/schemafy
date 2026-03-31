package com.schemafy.api.erd.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schemafy.api.erd.controller.dto.response.SchemaSnapshotsResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.api.testsupport.project.ProjectHttpTestSupport;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaCommand;
import com.schemafy.core.erd.schema.application.port.in.CreateSchemaUseCase;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameCommand;
import com.schemafy.core.erd.table.application.port.in.ChangeTableNameUseCase;
import com.schemafy.core.erd.table.application.port.in.CreateTableCommand;
import com.schemafy.core.erd.table.application.port.in.CreateTableUseCase;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("SchemaSnapshotOrchestrator 통합 테스트")
class SchemaSnapshotOrchestratorIntegrationTest extends ProjectHttpTestSupport {

  @Autowired
  private SchemaSnapshotOrchestrator schemaSnapshotOrchestrator;

  @Autowired
  private CreateSchemaUseCase createSchemaUseCase;

  @Autowired
  private CreateTableUseCase createTableUseCase;

  @Autowired
  private ChangeTableNameUseCase changeTableNameUseCase;

  @MockitoSpyBean
  private TableSnapshotOrchestrator tableSnapshotOrchestrator;

  @Test
  @DisplayName("초기 로드 중 concurrent mutation이 발생해도 revision과 snapshots는 같은 시점을 유지한다")
  void returnsConsistentRevisionAndSnapshotsDuringConcurrentMutation() throws Exception {
    String projectId = saveProject(
        saveWorkspace("schema_snapshot_workspace", "description").getId(),
        "schema_snapshot_project",
        "description")
        .getId();

    String schemaId = createSchemaUseCase.createSchema(new CreateSchemaCommand(
        projectId,
        "MySQL",
        "snapshot_schema",
        "utf8mb4",
        "utf8mb4_general_ci")).block().result().id();

    var tableResult = createTableUseCase.createTable(new CreateTableCommand(
        schemaId,
        "users",
        "utf8mb4",
        "utf8mb4_general_ci",
        null)).block().result();

    long revisionBeforeMutation = currentRevision(schemaId);
    String originalTableName = tableResult.name();
    String updatedTableName = "users_renamed";

    CountDownLatch snapshotReadStarted = new CountDownLatch(1);
    CountDownLatch allowSnapshotRead = new CountDownLatch(1);

    doAnswer(invocation -> Mono.defer(() -> {
      snapshotReadStarted.countDown();
      try {
        awaitReleased(allowSnapshotRead);
        @SuppressWarnings("unchecked")
        Mono<Map<String, TableSnapshotResponse>> realCall =
            (Mono<Map<String, TableSnapshotResponse>>) invocation.callRealMethod();
        return realCall;
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return Mono.error(ex);
      } catch (Throwable throwable) {
        return Mono.error(throwable);
      }
    })).when(tableSnapshotOrchestrator).getTableSnapshotsStrict(anyList());

    CompletableFuture<SchemaSnapshotsResponse> responseFuture = CompletableFuture.supplyAsync(
        () -> schemaSnapshotOrchestrator.getSchemaSnapshots(schemaId).block());

    await(snapshotReadStarted);

    CompletableFuture<Void> mutationFuture = CompletableFuture.runAsync(() -> changeTableNameUseCase
        .changeTableName(new ChangeTableNameCommand(
            tableResult.tableId(),
            updatedTableName))
        .block());
    allowSnapshotRead.countDown();

    mutationFuture.get(10, TimeUnit.SECONDS);
    SchemaSnapshotsResponse response = responseFuture.get(10, TimeUnit.SECONDS);

    assertThat(currentRevision(schemaId)).isEqualTo(revisionBeforeMutation + 1);
    assertThat(response.currentRevision()).isEqualTo(revisionBeforeMutation);
    assertThat(response.snapshots()).containsKey(tableResult.tableId());
    assertThat(response.snapshots().get(tableResult.tableId()).table().name())
        .isEqualTo(originalTableName)
        .isNotEqualTo(updatedTableName);
  }

  private long currentRevision(String schemaId) {
    return databaseClient.sql("""
        SELECT current_revision
        FROM schema_collaboration_state
        WHERE schema_id = :schemaId
        """)
        .bind("schemaId", schemaId)
        .map((row, metadata) -> ((Number) row.get("current_revision")).longValue())
        .one()
        .block();
  }

  private void await(CountDownLatch latch) throws InterruptedException {
    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
  }

  private void awaitReleased(CountDownLatch releaseLatch)
      throws InterruptedException {
    assertThat(releaseLatch.await(5, TimeUnit.SECONDS)).isTrue();
  }
}
