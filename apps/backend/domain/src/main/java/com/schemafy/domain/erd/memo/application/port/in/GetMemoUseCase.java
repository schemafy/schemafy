package com.schemafy.domain.erd.memo.application.port.in;

import com.schemafy.domain.erd.memo.domain.MemoDetail;

import reactor.core.publisher.Mono;

public interface GetMemoUseCase {

  Mono<MemoDetail> getMemo(GetMemoQuery query);

}
