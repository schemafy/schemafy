package com.schemafy.domain.erd.column.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeColumnPositionPort {

  Mono<Void> changeColumnPosition(String columnId, int seqNo);

}
