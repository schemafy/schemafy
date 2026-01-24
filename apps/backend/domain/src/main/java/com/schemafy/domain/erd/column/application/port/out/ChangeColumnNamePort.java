package com.schemafy.domain.erd.column.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeColumnNamePort {

  Mono<Void> changeColumnName(String columnId, String newName);

}
