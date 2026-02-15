package com.schemafy.domain.erd.table.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeTableNamePort {

  Mono<Void> changeTableName(String tableId, String newName);

}
