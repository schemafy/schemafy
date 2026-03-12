package com.schemafy.core.erd.memo.application.port.in;

import com.schemafy.core.erd.memo.domain.MemoDetail;

import reactor.core.publisher.Mono;

public interface GetMemoUseCase {

  Mono<MemoDetail> getMemo(GetMemoQuery query);

}
