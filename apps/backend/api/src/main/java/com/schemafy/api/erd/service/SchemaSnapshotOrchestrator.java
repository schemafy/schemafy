package com.schemafy.api.erd.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.schemafy.api.erd.controller.dto.response.SchemaSnapshotsResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionUseCase;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;

import reactor.core.publisher.Mono;

@Service
public class SchemaSnapshotOrchestrator {

  private final GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase;
  private final GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;
  private final TableSnapshotOrchestrator tableSnapshotOrchestrator;
  private final TransactionalOperator transactionalOperator;

  public SchemaSnapshotOrchestrator(
      GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase,
      GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase,
      TableSnapshotOrchestrator tableSnapshotOrchestrator,
      ReactiveTransactionManager transactionManager) {
    this.getSchemaWithRevisionUseCase = getSchemaWithRevisionUseCase;
    this.getTablesBySchemaIdUseCase = getTablesBySchemaIdUseCase;
    this.tableSnapshotOrchestrator = tableSnapshotOrchestrator;
    this.transactionalOperator = createReadTransactionalOperator(
        transactionManager);
  }

  public Mono<SchemaSnapshotsResponse> getSchemaSnapshots(String schemaId) {
    return Mono.defer(() -> getSchemaWithRevisionUseCase
        .getSchemaWithRevision(new GetSchemaQuery(schemaId))
        .flatMap(result -> {
          Mono<Map<String, TableSnapshotResponse>> snapshotsMono = getTablesBySchemaIdUseCase
              .getTablesBySchemaId(new GetTablesBySchemaIdQuery(result.schema().id()))
              .map(table -> table.id())
              .collectList()
              .flatMap(tableIds -> tableIds.isEmpty()
                  ? Mono.just(Map.<String, TableSnapshotResponse>of())
                  : tableSnapshotOrchestrator.getTableSnapshotsStrict(tableIds));

          return snapshotsMono.map(snapshots -> new SchemaSnapshotsResponse(
              result.currentRevision(),
              snapshots));
        }))
        .as(transactionalOperator::transactional);
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
