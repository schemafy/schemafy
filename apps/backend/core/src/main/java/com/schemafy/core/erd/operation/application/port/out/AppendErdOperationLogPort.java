package com.schemafy.core.erd.operation.application.port.out;

import com.schemafy.core.erd.operation.domain.ErdOperationLog;

import reactor.core.publisher.Mono;

public interface AppendErdOperationLogPort {

  Mono<ErdOperationLog> append(ErdOperationLog erdOperationLog);

}
