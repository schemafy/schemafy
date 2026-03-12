package com.schemafy.core.erd.memo.application.port.in;

import com.schemafy.core.erd.memo.domain.MemoComment;

import reactor.core.publisher.Mono;

public interface CreateMemoCommentUseCase {

  Mono<MemoComment> createMemoComment(CreateMemoCommentCommand command);

}
