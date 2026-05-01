package com.schemafy.core.erd.operation.application.port.out;

import java.util.List;

import com.schemafy.core.erd.operation.domain.ErdOperationLog;

import reactor.core.publisher.Mono;

public interface GetErdOperationsBySchemaIdPort {

  Mono<List<ErdOperationLog>> findOperationsBySchemaIdOrderByCommittedRevisionAsc(String schemaId);

}
