package com.schemafy.core.erd.column.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeColumnNamePort {

  Mono<Void> changeColumnName(String columnId, String newName);

}
