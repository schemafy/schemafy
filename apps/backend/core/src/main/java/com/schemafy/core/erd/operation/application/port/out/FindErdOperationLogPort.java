package com.schemafy.core.erd.operation.application.port.out;

import com.schemafy.core.erd.operation.domain.ErdOperationLog;

import reactor.core.publisher.Mono;

public interface FindErdOperationLogPort {

  Mono<ErdOperationLog> findByOpId(String opId);

}
