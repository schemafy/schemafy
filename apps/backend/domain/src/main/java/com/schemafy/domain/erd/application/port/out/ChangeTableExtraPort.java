package com.schemafy.domain.erd.application.port.out;

import reactor.core.publisher.Mono;

public interface ChangeTableExtraPort {

  Mono<Void> changeTableExtra(String tableId, String extra);
}
