package com.schemafy.domain.erd.schema.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.out.DeleteSchemaPort;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableCommand;
import com.schemafy.domain.erd.table.application.port.in.DeleteTableUseCase;
import com.schemafy.domain.erd.table.application.port.out.GetTablesBySchemaIdPort;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeleteSchemaService implements DeleteSchemaUseCase {

  private final TransactionalOperator transactionalOperator;
  private final DeleteSchemaPort deleteSchemaPort;
  private final GetTablesBySchemaIdPort getTablesBySchemaIdPort;
  private final DeleteTableUseCase deleteTableUseCase;

  @Override
  public Mono<Void> deleteSchema(DeleteSchemaCommand command) {
    String schemaId = command.schemaId();
    return getTablesBySchemaIdPort.findTablesBySchemaId(schemaId)
        .collectList()
        .flatMapMany(Flux::fromIterable)
        .concatMap(table -> deleteTableUseCase.deleteTable(new DeleteTableCommand(table.id())))
        .then(Mono.defer(() -> deleteSchemaPort.deleteSchema(schemaId)))
        .as(transactionalOperator::transactional);
  }

}
