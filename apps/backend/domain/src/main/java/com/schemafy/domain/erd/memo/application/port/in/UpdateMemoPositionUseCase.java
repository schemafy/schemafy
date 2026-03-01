package com.schemafy.domain.erd.memo.application.port.in;

import com.schemafy.domain.erd.memo.domain.Memo;

import reactor.core.publisher.Mono;

public interface UpdateMemoPositionUseCase {

  Mono<Memo> updateMemoPosition(UpdateMemoPositionCommand command);

}
