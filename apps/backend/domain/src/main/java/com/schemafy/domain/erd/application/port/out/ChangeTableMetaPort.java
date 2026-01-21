package com.schemafy.domain.erd.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeTableMetaPort {

  Mono<Void> changeTableMeta(String tableId, String charset, String collation);
}
