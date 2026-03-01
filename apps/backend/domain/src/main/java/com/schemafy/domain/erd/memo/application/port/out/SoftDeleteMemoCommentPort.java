package com.schemafy.domain.erd.memo.application.port.out;

import java.time.Instant;

import reactor.core.publisher.Mono;

public interface SoftDeleteMemoCommentPort {

  Mono<Void> softDeleteMemoComment(String commentId, Instant deletedAt);

}
