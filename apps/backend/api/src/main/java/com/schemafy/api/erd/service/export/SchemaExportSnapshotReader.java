package com.schemafy.api.erd.service.export;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.schemafy.api.erd.controller.dto.response.SchemaResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.api.erd.service.TableSnapshotOrchestrator;
import com.schemafy.core.erd.export.domain.SchemaExportSnapshot;
import com.schemafy.core.erd.index.domain.policy.IndexCapabilities;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionUseCase;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorUseCase;

import reactor.core.publisher.Mono;

@Service
public class SchemaExportSnapshotReader {

  private final GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase;
  private final GetProjectDbVendorUseCase getProjectDbVendorUseCase;
  private final GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;
  private final TableSnapshotOrchestrator tableSnapshotOrchestrator;
  private final SchemaExportSnapshotMapper snapshotMapper;
  private final TransactionalOperator transactionalOperator;

  public SchemaExportSnapshotReader(
      GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase,
      GetProjectDbVendorUseCase getProjectDbVendorUseCase,
      GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase,
      TableSnapshotOrchestrator tableSnapshotOrchestrator,
      SchemaExportSnapshotMapper snapshotMapper,
      ReactiveTransactionManager transactionManager) {
    this.getSchemaWithRevisionUseCase = getSchemaWithRevisionUseCase;
    this.getProjectDbVendorUseCase = getProjectDbVendorUseCase;
    this.getTablesBySchemaIdUseCase = getTablesBySchemaIdUseCase;
    this.tableSnapshotOrchestrator = tableSnapshotOrchestrator;
    this.snapshotMapper = snapshotMapper;
    this.transactionalOperator = createReadTransactionalOperator(
        transactionManager);
  }

  public Mono<SchemaExportSnapshotResult> readSchemaExportSnapshot(
      String schemaId) {
    return getSchemaWithRevisionUseCase
        .getSchemaWithRevision(new GetSchemaQuery(schemaId))
        .flatMap(result -> {
          SchemaResponse schema = SchemaResponse.from(
              result.schema(), result.currentRevision());
          var dbVendorMono = getProjectDbVendorUseCase.getProjectDbVendor(
              new GetProjectDbVendorQuery(result.schema().projectId()));
          Mono<Map<String, TableSnapshotResponse>> snapshotsMono = getTablesBySchemaIdUseCase
              .getTablesBySchemaId(new GetTablesBySchemaIdQuery(schemaId))
              .map(Table::id)
              .collectList()
              .flatMap(tableIds -> tableIds.isEmpty()
                  ? Mono.just(Map.<String, TableSnapshotResponse>of())
                  : tableSnapshotOrchestrator
                      .getTableSnapshotsStrict(tableIds));

          return Mono.zip(dbVendorMono, snapshotsMono)
              .map(tuple -> new SchemaExportSnapshotResult(
                  snapshotMapper.toSnapshot(
                      schema, tuple.getT2().values(), tuple.getT1().name()),
                  result.currentRevision(),
                  tuple.getT1().capabilities().indexes()));
        })
        .as(transactionalOperator::transactional);
  }

  private static TransactionalOperator createReadTransactionalOperator(
      ReactiveTransactionManager transactionManager) {
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    definition.setReadOnly(true);
    definition.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    definition.setName("schemaExportSnapshotRead");
    return TransactionalOperator.create(transactionManager, definition);
  }

  public record SchemaExportSnapshotResult(
      SchemaExportSnapshot snapshot,
      long currentRevision,
      IndexCapabilities indexCapabilities) {
  }

}
