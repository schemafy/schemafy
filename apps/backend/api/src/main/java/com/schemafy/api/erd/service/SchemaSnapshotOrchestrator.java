package com.schemafy.api.erd.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.schemafy.api.erd.controller.dto.response.SchemaResponse;
import com.schemafy.api.erd.controller.dto.response.SchemaSnapshotsResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.common.exception.DomainException;
import com.schemafy.core.erd.operation.application.port.out.FindSchemaCollaborationStatePort;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionUseCase;
import com.schemafy.core.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.core.erd.schema.domain.Schema;
import com.schemafy.core.erd.schema.domain.exception.SchemaErrorCode;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;

import reactor.core.publisher.Mono;

@Service
public class SchemaSnapshotOrchestrator {

  private final GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase;
  private final GetSchemaByIdPort getSchemaByIdPort;
  private final FindSchemaCollaborationStatePort findSchemaCollaborationStatePort;
  private final GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;
  private final TableSnapshotOrchestrator tableSnapshotOrchestrator;
  private final TransactionalOperator transactionalOperator;

  public SchemaSnapshotOrchestrator(
      GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase,
      GetSchemaByIdPort getSchemaByIdPort,
      FindSchemaCollaborationStatePort findSchemaCollaborationStatePort,
      GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase,
      TableSnapshotOrchestrator tableSnapshotOrchestrator,
      ReactiveTransactionManager transactionManager) {
    this.getSchemaWithRevisionUseCase = getSchemaWithRevisionUseCase;
    this.getSchemaByIdPort = getSchemaByIdPort;
    this.findSchemaCollaborationStatePort = findSchemaCollaborationStatePort;
    this.getTablesBySchemaIdUseCase = getTablesBySchemaIdUseCase;
    this.tableSnapshotOrchestrator = tableSnapshotOrchestrator;
    this.transactionalOperator = createReadTransactionalOperator(
        transactionManager);
  }

  public Mono<SchemaSnapshotsResponse> getSchemaSnapshots(String schemaId) {
    return getSchemaState(schemaId)
        .map(state -> new SchemaSnapshotsResponse(state.revision(),
            state.snapshots()));
  }

  /** REST-facing lookup. Goes through the access-gated
   * {@code GetSchemaWithRevisionUseCase}, so it only works with an
   * authenticated requester in context (an HTTP request). */
  public Mono<SchemaStateSnapshot> getSchemaState(String schemaId) {
    return buildSchemaState(schemaId, getSchemaWithRevisionUseCase
        .getSchemaWithRevision(new GetSchemaQuery(schemaId))
        .map(result -> new SchemaAndRevision(result.schema(),
            result.currentRevision())));
  }

  /** {@code ErdStateSnapshotWorker}-only lookup. Deliberately bypasses the
   * {@code @RequireProjectAccess} check that {@link #getSchemaState} goes
   * through: the worker runs on a {@code @Scheduled} poller thread with no
   * authenticated requester in Reactor Context, so that check always fails
   * there with "Project access requester is missing". This is safe because
   * the worker never answers a new user request; it only republishes the
   * full state of a mutation that was already authorized when it was
   * originally committed. Do not use this from a controller or any other
   * user-facing path. */
  public Mono<SchemaStateSnapshot> getSchemaStateForSnapshotWorker(
      String schemaId) {
    Mono<SchemaAndRevision> schemaAndRevisionMono = getSchemaByIdPort
        .findSchemaById(schemaId)
        .switchIfEmpty(Mono.error(new DomainException(
            SchemaErrorCode.NOT_FOUND, "Schema not found: " + schemaId)))
        .flatMap(schema -> findSchemaCollaborationStatePort
            .findBySchemaId(schemaId)
            .map(state -> new SchemaAndRevision(schema,
                state.currentRevision()))
            .defaultIfEmpty(new SchemaAndRevision(schema, 0L)));
    return buildSchemaState(schemaId, schemaAndRevisionMono);
  }

  private Mono<SchemaStateSnapshot> buildSchemaState(String schemaId,
      Mono<SchemaAndRevision> schemaAndRevisionMono) {
    return Mono.defer(() -> schemaAndRevisionMono
        .flatMap(schemaAndRevision -> {
          Mono<Map<String, TableSnapshotResponse>> snapshotsMono = getTablesBySchemaIdUseCase
              .getTablesBySchemaId(new GetTablesBySchemaIdQuery(schemaId))
              .map(table -> table.id())
              .collectList()
              .flatMap(tableIds -> tableIds.isEmpty()
                  ? Mono.just(Map.<String, TableSnapshotResponse>of())
                  : tableSnapshotOrchestrator.getTableSnapshotsStrict(tableIds));

          return snapshotsMono.map(snapshots -> new SchemaStateSnapshot(
              SchemaResponse.from(schemaAndRevision.schema()),
              schemaAndRevision.revision(), snapshots));
        }))
        .as(transactionalOperator::transactional);
  }

  private record SchemaAndRevision(Schema schema, long revision) {
  }

  private static TransactionalOperator createReadTransactionalOperator(
      ReactiveTransactionManager transactionManager) {
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    definition.setReadOnly(true);
    definition.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    definition.setName("schemaSnapshotRead");
    return TransactionalOperator.create(transactionManager, definition);
  }

}
