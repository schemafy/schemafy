package com.schemafy.domain.erd.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeColumnNamePort {

  Mono<Void> changeColumnName(String columnId, String newName);

}
