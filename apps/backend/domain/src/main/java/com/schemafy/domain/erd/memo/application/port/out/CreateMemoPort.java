package com.schemafy.domain.erd.memo.application.port.out;

import com.schemafy.domain.erd.memo.domain.Memo;

import reactor.core.publisher.Mono;

public interface CreateMemoPort {

  Mono<Memo> createMemo(Memo memo);

}
