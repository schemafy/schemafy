package com.schemafy.domain.erd.memo.application.port.out;

import com.schemafy.domain.erd.memo.domain.Memo;

import reactor.core.publisher.Mono;

public interface GetMemoByIdPort {

  Mono<Memo> findMemoById(String memoId);

}
