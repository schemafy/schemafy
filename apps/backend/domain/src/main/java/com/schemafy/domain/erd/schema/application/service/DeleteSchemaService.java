package com.schemafy.domain.erd.schema.application.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import com.schemafy.domain.common.MutationResult;
import com.schemafy.domain.common.exception.DomainException;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaCommand;
import com.schemafy.domain.erd.schema.application.port.in.DeleteSchemaUseCase;
import com.schemafy.domain.erd.schema.application.port.out.DeleteSchemaPort;
import com.schemafy.domain.erd.schema.application.port.out.GetSchemaByIdPort;
import com.schemafy.domain.erd.schema.domain.exception.SchemaErrorCode;
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
  private final GetSchemaByIdPort getSchemaByIdPort;
  private final GetTablesBySchemaIdPort getTablesBySchemaIdPort;
  private final DeleteTableUseCase deleteTableUseCase;

  @Override
  public Mono<MutationResult<Void>> deleteSchema(DeleteSchemaCommand command) {
    String schemaId = command.schemaId();
    Set<String> affectedTableIds = ConcurrentHashMap.newKeySet();
    Mono<Void> ensureExists = getSchemaByIdPort.findSchemaById(schemaId)
        .switchIfEmpty(Mono.error(new DomainException(SchemaErrorCode.NOT_FOUND, "Schema not found: " + schemaId)))
        .then();
    return ensureExists
        .then(getTablesBySchemaIdPort.findTablesBySchemaId(schemaId)
            .collectList())
        .flatMapMany(Flux::fromIterable)
        .concatMap(table -> {
          affectedTableIds.add(table.id());
          return deleteTableUseCase.deleteTable(new DeleteTableCommand(table.id()))
              .doOnNext(result -> affectedTableIds.addAll(result.affectedTableIds()))
              .then();
        })
        .then(Mono.defer(() -> deleteSchemaPort.deleteSchema(schemaId)))
        .then(Mono.fromCallable(() -> MutationResult.<Void>of(null, affectedTableIds)))
        .as(transactionalOperator::transactional);
  }

}
