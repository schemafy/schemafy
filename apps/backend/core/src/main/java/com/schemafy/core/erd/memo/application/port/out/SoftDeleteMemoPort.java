package com.schemafy.core.erd.memo.application.port.out;

import java.time.Instant;

import reactor.core.publisher.Mono;

public interface SoftDeleteMemoPort {

  Mono<Void> softDeleteMemo(String memoId, Instant deletedAt);

}
