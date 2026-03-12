package com.schemafy.core.erd.memo.application.port.out;

import com.schemafy.core.erd.memo.domain.Memo;

import reactor.core.publisher.Mono;

public interface CreateMemoPort {

  Mono<Memo> createMemo(Memo memo);

}
