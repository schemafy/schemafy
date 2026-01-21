package com.schemafy.domain.erd.application.port.out;

import reactor.core.publisher.Mono;

public interface TableExistsPort {

  Mono<Boolean> existsBySchemaIdAndName(String schemaId, String name);

}
