package com.schemafy.api.erd.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.schemafy.api.erd.controller.dto.response.SchemaDdlExportResponse;
import com.schemafy.api.erd.controller.dto.response.SchemaResponse;
import com.schemafy.api.erd.controller.dto.response.TableSnapshotResponse;
import com.schemafy.api.erd.service.ddl.DdlExportSnapshotMapper;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlCommand;
import com.schemafy.core.erd.ddl.application.port.in.GenerateSchemaDdlUseCase;
import com.schemafy.core.erd.ddl.domain.DdlExportVendor;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaQuery;
import com.schemafy.core.erd.schema.application.port.in.GetSchemaWithRevisionUseCase;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdQuery;
import com.schemafy.core.erd.table.application.port.in.GetTablesBySchemaIdUseCase;
import com.schemafy.core.erd.table.domain.Table;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorQuery;
import com.schemafy.core.erd.vendor.application.port.in.GetProjectDbVendorUseCase;

import reactor.core.publisher.Mono;

@Service
public class SchemaDdlExportOrchestrator {

  private final GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase;
  private final GetProjectDbVendorUseCase getProjectDbVendorUseCase;
  private final GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase;
  private final TableSnapshotOrchestrator tableSnapshotOrchestrator;
  private final GenerateSchemaDdlUseCase generateSchemaDdlUseCase;
  private final DdlExportSnapshotMapper ddlExportSnapshotMapper;
  private final TransactionalOperator transactionalOperator;

  public SchemaDdlExportOrchestrator(
      GetSchemaWithRevisionUseCase getSchemaWithRevisionUseCase,
      GetProjectDbVendorUseCase getProjectDbVendorUseCase,
      GetTablesBySchemaIdUseCase getTablesBySchemaIdUseCase,
      TableSnapshotOrchestrator tableSnapshotOrchestrator,
      GenerateSchemaDdlUseCase generateSchemaDdlUseCase,
      DdlExportSnapshotMapper ddlExportSnapshotMapper,
      ReactiveTransactionManager transactionManager) {
    this.getSchemaWithRevisionUseCase = getSchemaWithRevisionUseCase;
    this.getProjectDbVendorUseCase = getProjectDbVendorUseCase;
    this.getTablesBySchemaIdUseCase = getTablesBySchemaIdUseCase;
    this.tableSnapshotOrchestrator = tableSnapshotOrchestrator;
    this.generateSchemaDdlUseCase = generateSchemaDdlUseCase;
    this.ddlExportSnapshotMapper = ddlExportSnapshotMapper;
    this.transactionalOperator = createReadTransactionalOperator(
        transactionManager);
  }

  public Mono<SchemaDdlExportResponse> exportSchemaDdl(String schemaId,
      String targetDbVendor) {
    return Mono.defer(() -> {
      DdlExportVendor exportVendor = DdlExportVendor.of(targetDbVendor);
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
                .flatMap(tuple -> generateSchemaDdlUseCase
                    .generateSchemaDdl(new GenerateSchemaDdlCommand(
                        ddlExportSnapshotMapper.toSnapshot(
                            schema, tuple.getT2().values(), tuple.getT1().name()),
                        exportVendor))
                    .map(ddl -> new SchemaDdlExportResponse(
                        schema.id(),
                        result.currentRevision(),
                        exportVendor.value(),
                        ddl)));
          });
    })
        .as(transactionalOperator::transactional);
  }

  private static TransactionalOperator createReadTransactionalOperator(
      ReactiveTransactionManager transactionManager) {
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    definition.setReadOnly(true);
    definition.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    definition.setName("schemaDdlExportRead");
    return TransactionalOperator.create(transactionManager, definition);
  }

}
