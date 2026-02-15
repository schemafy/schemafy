package com.schemafy.domain.erd.index.application.port.out;

import reactor.core.publisher.Mono;

public interface IndexExistsPort {

  Mono<Boolean> existsByTableIdAndName(String tableId, String name);

  Mono<Boolean> existsByTableIdAndNameExcludingId(
      String tableId,
      String name,
      String indexId);

}
