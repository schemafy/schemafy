package com.schemafy.domain.erd.memo.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteMemoUseCase {

  Mono<Void> deleteMemo(DeleteMemoCommand command);

}
