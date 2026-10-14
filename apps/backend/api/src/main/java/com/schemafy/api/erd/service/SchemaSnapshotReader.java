package com.schemafy.api.erd.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionUseCase;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;
import com.schemafy.core.erd.table.domain.Table;

import reactor.core.publisher.Mono;

@Service
public class SchemaSnapshotReader {

  private final GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase;
  private final GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;
  private final TableSnapshotOrchestrator tableSnapshotOrchestrator;
  private final TransactionalOperator transactionalOperator;

  public SchemaSnapshotReader(
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

  public Mono<SchemaSnapshotReadResult> readSchemaSnapshot(String schemaId) {
    return Mono.defer(() -> getSchemaWithRevisionUseCase
        .getSchemaWithRevision(new GetSchemaQuery(schemaId))
        .flatMap(result -> readTableSnapshots(schemaId)
            .map(tableSnapshots -> new SchemaSnapshotReadResult(
                result.schema(), result.currentRevision(), tableSnapshots))))
        .as(transactionalOperator::transactional);
  }

  private Mono<Map<String, TableSnapshotResponse>> readTableSnapshots(
      String schemaId) {
    return getTablesBySchemaIdUseCase
        .getTablesBySchemaId(new GetTablesBySchemaIdQuery(schemaId))
        .map(Table::id)
        .collectList()
        .flatMap(tableIds -> tableIds.isEmpty()
            ? Mono.just(Map.<String, TableSnapshotResponse>of())
            : tableSnapshotOrchestrator.getTableSnapshotsStrict(tableIds));
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
