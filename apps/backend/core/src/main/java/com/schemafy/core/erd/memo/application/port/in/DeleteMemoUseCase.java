package com.schemafy.core.erd.memo.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteMemoUseCase {

  Mono<Void> deleteMemo(DeleteMemoCommand command);

}
