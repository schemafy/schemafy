package com.schemafy.core.erd.table.application.port.out;

import reactor.core.publisher.Mono;

public interface CascadeDeleteTablesBySchemaIdPort {

  Mono<Void> cascadeDeleteBySchemaId(String schemaId);

}
