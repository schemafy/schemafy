package com.schemafy.core.erd.memo.application.port.in;

import com.schemafy.core.erd.memo.domain.MemoDetail;

import reactor.core.publisher.Mono;

public interface CreateMemoUseCase {

  Mono<MemoDetail> createMemo(CreateMemoCommand command);

}
