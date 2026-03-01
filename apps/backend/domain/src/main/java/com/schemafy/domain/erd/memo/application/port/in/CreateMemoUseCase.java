package com.schemafy.domain.erd.memo.application.port.in;

import com.schemafy.domain.erd.memo.domain.MemoDetail;

import reactor.core.publisher.Mono;

public interface CreateMemoUseCase {

  Mono<MemoDetail> createMemo(CreateMemoCommand command);

}
